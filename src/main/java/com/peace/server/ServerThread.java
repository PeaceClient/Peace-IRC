package com.peace.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.*;
import com.peace.packets.s2c.ChatS2CPacket;
import com.peace.packets.s2c.LoginS2CPacket;
import com.peace.packets.s2c.PlayerPositionS2CPacket;
import com.peace.packets.s2c.ServerMessageS2CPacket;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ServerThread implements Runnable {
    private Socket clientSocket;
    private final ServerMain serverMain;
    private PrintWriter out;
    private BufferedReader in;

    private final BlockingQueue<Packet> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running;

    private boolean loggedIn;
    // TODO: timeout after 10sec if !loggedIn
    private long connectMillis;

    private String username;

    private volatile boolean positionChanged;
    private volatile @Nullable Vec2i position;
    // Null when not breaking!
    private volatile boolean breakingChanged;
    private volatile @Nullable BlockPos breakingPos;
    private volatile float breakingProgress;

    // 1 second cooldown
    private volatile long lastChatMessage;

    public ServerThread(Socket clientSocket, ServerMain serverMain) {
        this.clientSocket = clientSocket;
        this.serverMain = serverMain;

        this.loggedIn = false;
        this.connectMillis = System.currentTimeMillis();
    }

    /**
     * Packet docs
     * {type: "type", data: {}}
     */
    @Override
    public void run() {
        this.running = true;

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            startWriterThread();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Recieved: " + inputLine);

                JsonElement element = JsonParser.parseString(inputLine);

                JsonObject root = element.getAsJsonObject();

                Packet packet = PacketFactory.createPacket(root);
                if (loggedIn) handlePacket(packet);
                else handlePacketNotLoggedIn(packet);
            }
        } catch (Exception e) {
            System.out.println("Failure in handling packets");
            e.printStackTrace();
        } finally {
            disconnect();
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

        if (serverMain.nameMap.containsKey(this.username)) {
            System.out.println("Player trying to connect with existing username!");
            sendPacket(new LoginS2CPacket(false));
            this.disconnect();
        } else {
            System.out.println("Player " + loginC2SPacket.getUsername() + " logged in!");
            this.loggedIn = true;
            this.username = loginC2SPacket.getUsername();
            serverMain.nameMap.put(this.username, this);

            sendPacket(new LoginS2CPacket(true));
        }
    }

    public void handlePacket(Packet packet) {
        if (packet instanceof RequestPlayerPositionC2SPacket requestPlayerPositionC2SPacket) {
            String requestedUser = requestPlayerPositionC2SPacket.getUsername();
            ServerThread otherThread = serverMain.nameMap.get(requestedUser);
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
        if (packet instanceof PlayerPositionC2SPacket updatePositionC2SPacket) {
            Vec2i ownPosition = this.position;
            if (updatePositionC2SPacket.getPosition() == null) return;

            // less than N blocks, prevents spam
            double d = serverMain.getConfig().getPositionUpdateDistance();
            if (ownPosition != null && ownPosition.squaredDistanceTo(updatePositionC2SPacket.getPosition()) < d*d) return;

            this.position = updatePositionC2SPacket.getPosition();
            this.positionChanged = true;
            return;
        }
        if (packet instanceof BreakingC2SPacket breakingC2SPacket) {
            this.breakingPos = breakingC2SPacket.getPosition();
            this.breakingProgress = breakingC2SPacket.getBreakingProgress();
            System.out.println("Refreshed breaking pos!");
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
                for (ServerThread player : serverMain.nameMap.values()) {
                    player.sendPacket(new ChatS2CPacket(this.username, chatC2SPacket.getMessage()));
                }
            }
        }
    }

    public void sendChatMessage(String text) {
        sendPacket(new ServerMessageS2CPacket(text));
    }

    public void sendPacket(Packet packet) {
        if (running && !outgoingQueue.offer(packet)) {
            System.out.println("Issue with sending packet!");
        }
    }

    public boolean shouldUpdatePositionAndReset() {
        boolean shouldUpdate = this.positionChanged;
        this.positionChanged = false;
        return shouldUpdate;
    }

    public @Nullable Vec2i getPosition() {
        return this.position;
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


    public void disconnect() {
        this.running = false;
        try {
            serverMain.nameMap.remove(this.username);
            if (in != null) in.close();
            if (out != null) out.close();
            in = null;
            out = null;
            if (!clientSocket.isClosed()) clientSocket.close();
            clientSocket = null;
            System.out.println("Closed connection for player: " + (username == null ? "None" : username));
        } catch (IOException ignored) {
        }
    }
}
