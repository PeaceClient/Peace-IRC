package com.peace.server;

import com.peace.VersionFeatures;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.*;
import com.peace.packets.s2c.*;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCInventory;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class IRCServerThread implements Runnable {
    private Socket clientSocket;
    private final IRCServerMain serverMain;

    private DataOutputStream out;
    private DataInputStream in;

    private final BlockingQueue<Packet> outgoingQueue = new LinkedBlockingQueue<>();
    private volatile boolean running;

    private boolean loggedIn;
    private final long connectMillis;

    private int protocolVersion;

    private String username;
    private String server;


    // Null when not breaking!
    private volatile boolean breakingChanged;
    private volatile @Nullable IRCBlockPos breakingPos;
    private volatile float breakingProgress;

    private volatile long lastChatMessage;
    //private volatile long lastInventoryRequest; // TODO: map a delay for this

    public IRCServerThread(Socket clientSocket, IRCServerMain serverMain) {
        this.clientSocket = clientSocket;
        this.serverMain = serverMain;

        this.loggedIn = false;
        this.connectMillis = System.currentTimeMillis(); // technically wrong but after init .run() gets called anyways
    }

    @Override
    public void run() {
        if (this.running) throw new IllegalStateException("IRCServerThread already running");
        this.serverMain.notLoggedInSet.add(this);
        this.running = true;

        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new DataOutputStream(clientSocket.getOutputStream());

            startWriterThread();

            while (running) {
                int length;
                try {
                    length = in.readInt();
                } catch (EOFException exception) {
                    // graceful end
                    break;
                }

                if (length < 0) throw new IllegalArgumentException("Packet size is negative?");
                if (length > 2000000) {
                    this.disconnect("Packet too large! Max size: 2000000 bytes");
                    return;
                }

                byte[] payload = new byte[length];
                in.readFully(payload);
                // wrapper
                DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));

                Packet packet = PacketFactory.createPacket(payloadIn);

                if (packet instanceof DisconnectC2SPacket) {
                    this.disconnect("User disconnect");
                    return;
                }

                if (loggedIn) handlePacket(packet);
                else handlePacketNotLoggedIn(packet);
            }
        } catch (Exception e) {
            System.out.println("Failure in handling packets");
            e.printStackTrace(System.out);
        } finally {
            disconnect("User disconnect");
        }
    }

    private void startWriterThread() {
        Thread writer = new Thread(() -> {
            try {
                while (running) {
                    Packet packet = outgoingQueue.take(); // blocks
                    writeInternal(packet);
                }
            } catch (Exception e) {
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
            this.serverMain.report(this.server, updatePositionC2SPacket.getUsername(), updatePositionC2SPacket.getPosition(),
                    updatePositionC2SPacket.getHealth(), updatePositionC2SPacket.getEquipment(), now);
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
        if (packet instanceof CustomCallbackC2SPacket customCallbackC2SPacket) {
            this.serverMain.handleCallback(this, customCallbackC2SPacket.getData());
        }
    }

    private void writeInternal(Packet packet) throws IOException {
        if (clientSocket != null && !clientSocket.isClosed() && out != null) {
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            DataOutputStream tmpOut = new DataOutputStream(byteArrayStream);

            PacketFactory.serializePacket(tmpOut, packet);
            tmpOut.flush();

            byte[] payload = byteArrayStream.toByteArray();
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
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

    public IRCBlockPos getBreakingPosition() {
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

        try {
            writeInternal(new DisconnectS2CPacket(reason));
        } catch (IOException ignored) {
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
