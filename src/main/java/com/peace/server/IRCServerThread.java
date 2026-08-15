package com.peace.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.*;
import com.peace.packets.s2c.ChatS2CPacket;
import com.peace.packets.s2c.DisconnectS2CPacket;
import com.peace.packets.s2c.LoginSuccessS2CPacket;
import com.peace.packets.s2c.ServerMessageS2CPacket;
import com.peace.util.BlockPos;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class IRCServerThread implements Runnable {
    private Socket clientSocket;
    private final IRCServerMain serverMain;
    private PrintWriter out;
    private BufferedReader in;

    private final BlockingQueue<Packet> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running;

    private boolean loggedIn;
    private final long connectMillis;

    private String username;
    private String server;

    private final Map<String, BlockPos> cachedBlockPositions = new ConcurrentHashMap<>();
    private volatile boolean positionsChanged;

    // Null when not breaking!
    private volatile boolean breakingChanged;
    private volatile @Nullable BlockPos breakingPos;
    private volatile float breakingProgress;

    // 1 second cooldown
    private volatile long lastChatMessage;

    public IRCServerThread(Socket clientSocket, IRCServerMain serverMain) {
        this.clientSocket = clientSocket;
        this.serverMain = serverMain;

        this.loggedIn = false;
        this.connectMillis = System.currentTimeMillis(); // technically wrong but after init .run() gets called anyways
    }

    /**
     * Packet docs
     * {type: "type", data: {}}
     */
    @Override
    public void run() {
        this.serverMain.notLoggedInSet.add(this);
        this.running = true;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            startWriterThread();

            String inputLine;
            while (in != null && (inputLine = in.readLine()) != null) {
                JsonElement element = JsonParser.parseString(inputLine);

                JsonObject root = element.getAsJsonObject();

                Packet packet = PacketFactory.createPacket(root);

                if (packet instanceof DisconnectC2SPacket) {
                    this.disconnect("User disconnect");
                    return;
                }

                if (loggedIn) handlePacket(packet);
                else handlePacketNotLoggedIn(packet);
            }
        } catch (Exception e) {
            System.out.println("Failure in handling packets");
            e.printStackTrace();
        } finally {
            disconnect("User disconnect");
        }
    }

    private void startWriterThread() {
        Thread writer = new Thread(() -> {
            try {
                while (running) {
                    Packet packet = outgoingQueue.take(); // blocks
                    if (clientSocket != null && !clientSocket.isClosed() && out != null) {
                        out.println(PacketFactory.serializePacket(packet));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                disconnect();
            }
        }, "Server-Writer-" + clientSocket.getPort());
        writer.setDaemon(true);
        writer.start();
    }

    private void handlePacketNotLoggedIn(Packet packet) {
        if (!(packet instanceof LoginC2SPacket loginC2SPacket)) return;
        if (!Objects.equals(loginC2SPacket.getPassword(), serverMain.getConfig().getPassword())) return;

        if (serverMain.getUsers(loginC2SPacket.getServer()).containsKey(loginC2SPacket.getUsername())) {
            System.out.println("Player trying to connect with existing username!");
            this.disconnect("Username already logged on!");
        } else {
            this.loggedIn = true;
            this.username = loginC2SPacket.getUsername();
            this.server = loginC2SPacket.getServer();

            System.out.println("Player " + username + " logged in server " + server);

            this.serverMain.notLoggedInSet.remove(this);
            serverMain.add(this);

            sendPacket(new LoginSuccessS2CPacket());
        }
    }

    public void handlePacket(Packet packet) {
       /* if (packet instanceof RequestPlayerPositionC2SPacket requestPlayerPositionC2SPacket) {
            String requestedUser = requestPlayerPositionC2SPacket.getUsername();
            IRCServerThread otherThread = serverMain.nameMap.get(requestedUser);
            if (otherThread == null) {
                sendChatMessage("No player with name found!");
                return;
            }

            if (otherThread.position == null) {
                sendChatMessage("No position received from other player!");
                return;
            }

            sendPacket(new PlayerPositionS2CPacket(otherThread.position, requestedUser));
            return;
        }

        */
        if (packet instanceof SeenEntityC2SPacket updatePositionC2SPacket) {
            if (updatePositionC2SPacket.getPosition() == null) return;
            if (Math.abs(System.currentTimeMillis() - updatePositionC2SPacket.getSeen()) > 1000)
                return; // 1 sec difference, fake timing
            IRCServerMain.EntityState serverPosition = serverMain.getEntityState(server, updatePositionC2SPacket.getUsername());
            if (serverPosition == null || serverPosition.millis() < updatePositionC2SPacket.getSeen()) {
                this.serverMain.report(this.server, updatePositionC2SPacket.getUsername(), updatePositionC2SPacket.getPosition(), updatePositionC2SPacket.getSeen());
            }
            return;
        }
        if (packet instanceof BreakingC2SPacket breakingC2SPacket) {
            if (Objects.equals(this.breakingPos, breakingC2SPacket.getPosition())) {
                // equal progress and pos
                if (this.breakingProgress == breakingC2SPacket.getBreakingProgress()) return;
            }

            this.breakingPos = breakingC2SPacket.getPosition();
            this.breakingProgress = breakingC2SPacket.getBreakingProgress();
            this.breakingChanged = true;
            return;
        }
        if (packet instanceof ChatC2SPacket chatC2SPacket) {
            long now = System.currentTimeMillis();
            long cooldown = serverMain.getConfig().getChatCooldownMillis();
            if (now - lastChatMessage < cooldown) {
                long msLeft = cooldown - (now-lastChatMessage);
                sendPacket(new ServerMessageS2CPacket(String.format("You are on cooldown for %.1f seconds!", (double)(msLeft) / 1000)));
            } else {
                lastChatMessage = now;
                for (IRCServerThread player : serverMain.getUsers(this.server).values()) {
                    player.sendPacket(new ChatS2CPacket(this.username, chatC2SPacket.getMessage()));
                }
            }
        }
    }

    public void sendServerMessage(String text) {
        sendPacket(new ServerMessageS2CPacket(text));
    }

    public void sendPacket(Packet packet) {
        if (running && !outgoingQueue.offer(packet)) {
            System.out.println("Issue with sending packet!");
        }
    }

    public boolean shouldUpdatePositionAndReset() {
        boolean shouldUpdate = this.positionsChanged;
        this.positionsChanged = false;
        return shouldUpdate;
    }

    public Map<String, BlockPos> getCachedBlockPositions() {
        return cachedBlockPositions;
    }

    public boolean shouldUpdateBreakingAndReset() {
        boolean shouldUpdate = this.breakingChanged;
        this.breakingChanged = false;
        return shouldUpdate;
    }

    public BlockPos getBreakingPosition() {
        return this.breakingPos;
    }

    public float getBreakingProgress() {
        return this.breakingProgress;
    }

    public String getUsername() {
        return this.username;
    }

    public String getServer() {
        return this.server;
    }

    public long getConnectMillis() {
        return this.connectMillis;
    }

    public void disconnect() {
        disconnect("unspecified");
    }

    public void disconnect(String reason) {
        if (!this.running) return;
        this.running = false;
        if (clientSocket != null && !clientSocket.isClosed() && out != null) {
            out.println(PacketFactory.serializePacket(new DisconnectS2CPacket(reason)));
        }
        try {
            this.serverMain.notLoggedInSet.remove(this);
            if (this.server != null && this.username != null) serverMain.getUsers(this.server).remove(this.username);
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close();
            in = null;
            out = null;
            clientSocket = null;
            System.out.println("Closed connection for player: " + (username == null ? "None" : username));
        } catch (IOException ignored) {
        }
    }
}
