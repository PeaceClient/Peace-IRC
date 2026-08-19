package com.peace.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.VersionFeatures;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.*;
import com.peace.packets.s2c.*;
import com.peace.util.BlockPos;
import com.peace.util.IRCInventory;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
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

    private int protocolVersion;

    private String username;
    private String server;


    // Null when not breaking!
    private volatile boolean breakingChanged;
    private volatile @Nullable BlockPos breakingPos;
    private volatile float breakingProgress;

    private volatile long lastChatMessage;
    private volatile long lastInventoryRequest; // TODO: map a delay for this

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
        if (!Objects.equals(loginC2SPacket.getPassword(), serverMain.getConfig().getPassword())) {
            System.out.println("Player typed in invalid password with user: " + loginC2SPacket.getUsername());
            this.disconnect("Wrong password!");
            return;
        }

        if (serverMain.getUsers(loginC2SPacket.getServer()).containsKey(loginC2SPacket.getUsername())) {
            System.out.println("Player trying to connect with existing username!");
            this.disconnect("Username already logged on!");
        } else {
            this.loggedIn = true;
            this.username = loginC2SPacket.getUsername();
            this.server = loginC2SPacket.getServer();
            this.protocolVersion = loginC2SPacket.getProtocolVersion();
            System.out.println("Player " + username + " logged in server " + server);

            this.serverMain.notLoggedInSet.remove(this);

            sendPacket(new LoginSuccessS2CPacket());

            // sends all users to the current player
            serverMain.broadcastAllIRCUsers(this);
            // send self to all & self
            serverMain.add(this);

            if (this.protocolVersion > serverMain.protocolVersion) {
                sendPacket(new ServerMessageS2CPacket(String.format("Server running on version %d while client is ahead at version %d!", serverMain.protocolVersion, this.protocolVersion)));
            } else if (this.protocolVersion < serverMain.protocolVersion){
                sendPacket(new ServerMessageS2CPacket(String.format("Server running on version %d while client is behind at version %d!", serverMain.protocolVersion, this.protocolVersion)));
            }
        }
    }

    public void handlePacket(Packet packet) {
        long now = System.currentTimeMillis();
        if (packet instanceof SeenEntityC2SPacket updatePositionC2SPacket) {
            IRCServerMain.EntityState serverPosition = serverMain.getEntityState(server, updatePositionC2SPacket.getUsername());
            if (serverPosition == null || serverPosition.millis() < now) {
                this.serverMain.report(this.server, updatePositionC2SPacket.getUsername(), updatePositionC2SPacket.getPosition(), now);
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
        if (packet instanceof PrivateMessageC2SPacket privateMessageC2SPacket) {
            long cooldown = serverMain.getConfig().getChatCooldownMillis();
            if (now - lastChatMessage < cooldown) {
                long msLeft = cooldown - (now-lastChatMessage);
                sendPacket(new ServerMessageS2CPacket(String.format("You are on cooldown for %.1f seconds!", (double)(msLeft) / 1000)));
            } else {
                lastChatMessage = now;

                IRCServerThread target = serverMain.getUsers(this.server).get(privateMessageC2SPacket.getTarget());
                if (target == null) {
                    sendPacket(new ServerMessageS2CPacket(String.format("%s is not online!", privateMessageC2SPacket.getTarget())));
                    return;
                }
                if (!target.hasFeature(VersionFeatures.PRIVATE_MESSAGES)) {
                    sendPacket(new ServerMessageS2CPacket(String.format("%s's client doesn't support private messages!", privateMessageC2SPacket.getTarget())));
                    return;
                }
                target.sendPacket(new PrivateMessageS2CPacket(this.getUsername(), privateMessageC2SPacket.getMessage(), false));
                this.sendPacket(new PrivateMessageS2CPacket(target.getUsername(), privateMessageC2SPacket.getMessage(), true));
            }
        }
        if (packet instanceof RequestPlayerInventoryC2SPacket requestPlayerInventoryC2SPacket) {
            IRCServerThread target = serverMain.getUsers(this.server).get(requestPlayerInventoryC2SPacket.getUsername());
            if (target == null) {
                sendServerMessage("No player with name: " + requestPlayerInventoryC2SPacket.getUsername() + " was found!");
                return;
            }
            if (!target.hasFeature(VersionFeatures.INVENTORY_REQUESTS)) {
                sendPacket(new ServerMessageS2CPacket(String.format("%s's client doesn't support inventory sharing!", target.getUsername())));
                return;
            }

            this.serverMain.sendInventoryRequest(this, target);
        }
        if (packet instanceof SendPlayerInventoryC2SPacket sendPlayerInventoryC2SPacket) {
            int id = sendPlayerInventoryC2SPacket.getId();
            IRCInventory inventory = sendPlayerInventoryC2SPacket.getInventory();

            this.serverMain.fulfillInventoryRequest(this, id, inventory);
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

    public int getProtocolVersion() {
        return protocolVersion;
    }

    public boolean hasFeature(int feature) {
        return feature <= getProtocolVersion();
    }

    public void disconnect() {
        disconnect("unspecified");
    }

    public void disconnect(String reason) {
        disconnect(reason, true);
    }

    public void disconnect(String reason, boolean remove) {
        if (!this.running) return;
        this.running = false;
        if (clientSocket != null && !clientSocket.isClosed() && out != null) {
            out.println(PacketFactory.serializePacket(new DisconnectS2CPacket(reason)));
        }
        try {
            if (remove) this.serverMain.remove(this);
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
