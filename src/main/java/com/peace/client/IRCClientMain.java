package com.peace.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.DisconnectC2SPacket;
import com.peace.packets.c2s.LoginC2SPacket;
import com.peace.packets.s2c.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.*;

public class IRCClientMain {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    private final BlockingQueue<Packet> incomingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> outgoingQueue = new LinkedBlockingQueue<>();

    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running;

    private final String addr;
    private final int port;
    private final String username;
    private final String password;
    private final String server;
    private final IRCClientEventHandler eventHandler;

    private boolean loggedIn;

    public boolean debug;

    // TODO: input validation
    public IRCClientMain(String addr, int port, String username, String password, String server, IRCClientEventHandler eventHandler) {
        this.addr = addr;
        this.port = port;
        this.username = username;
        this.password = password;
        this.server = server;
        this.eventHandler = eventHandler;

        this.loggedIn = false;
    }

    public void start() throws IOException {
        if (this.running) throw new IllegalStateException("Client is already running");
        this.running = true;

        socket = new Socket(addr, port);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        startReaderThread();

        startWriterThread();

        startTickLoop();

        doLogin();
    }

    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    JsonObject root = JsonParser.parseString(line).getAsJsonObject();
                    Packet packet = PacketFactory.createPacket(root);

                    if (packet instanceof DisconnectS2CPacket disconnectS2CPacket) {
                        eventHandler.onKick(this, disconnectS2CPacket.getReason());
                        this.disconnect();
                        return;
                    }

                    incomingQueue.offer(packet);
                }
            } catch (Exception exception) {
                System.out.println("Error with reading packet");
            } finally {
                disconnect();
            }
        }, "IRC-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void startWriterThread() {
        Thread writer = new Thread(() -> {
            try {
                while (running) {
                    Packet packet = outgoingQueue.take();
                    if (socket.isConnected() && out != null) {
                        out.println(PacketFactory.serializePacket(packet));
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                disconnect();
            }
        }, "IRC-Writer");
        writer.setDaemon(true);
        writer.start();
    }

    private void startTickLoop() {
        // Runs every 50ms (20 Hz)
        tickExecutor.scheduleAtFixedRate(() -> {
            try {
                eventHandler.tick(this);

                Packet incoming;
                while ((incoming = incomingQueue.poll()) != null) {
                    boolean cancelled = eventHandler.onPacketReceive(this, incoming);
                    if (!cancelled) this.handlePacket(incoming);
                }
            } catch (Exception e) {
                disconnect();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    // TODO: err/info/warn debugs on event handler
    public void handlePacket(Packet packet) {
        if (!loggedIn) {
            if (packet instanceof LoginSuccessS2CPacket) {
                System.out.println("Logged in!");
                loggedIn = true;
                eventHandler.postLogin(this);
            }
            return;
        }

        if (packet instanceof BreakingS2CPacket breakingS2CPacket) {
            eventHandler.onProgressUpdate(this, breakingS2CPacket.getUsername(), breakingS2CPacket.getPosition(), breakingS2CPacket.getBreakingProgress());
        }

        if (packet instanceof PlayerPositionS2CPacket playerPositionS2CPacket) {
            eventHandler.onPositionReceive(this, playerPositionS2CPacket.getUsername(), playerPositionS2CPacket.getPosition());
        }

        if (packet instanceof ServerMessageS2CPacket serverMessageS2CPacket) {
            eventHandler.onServerMessage(this, serverMessageS2CPacket.getMessage());
        }

        if (packet instanceof ChatS2CPacket chatS2CPacket) {
            eventHandler.onIrcChat(this, chatS2CPacket.getUsername(), chatS2CPacket.getMessage());
        }

        if (packet instanceof IRCUsersS2CPacket ircUsersS2CPacket) {
            eventHandler.onIRCUserUpdate(this, ircUsersS2CPacket.getUsernames(), ircUsersS2CPacket.getAction(), ircUsersS2CPacket.shouldAnnounce());
        }

        if (packet instanceof PrivateMessageS2CPacket privateMessageS2CPacket) {
            eventHandler.onPrivateMessage(this, privateMessageS2CPacket.getSender(), privateMessageS2CPacket.getMessage(), privateMessageS2CPacket.isOwnMessage());
        }

        if (packet instanceof RequestPlayerInventoryS2CPacket requestPlayerInventoryS2CPacket) {
            eventHandler.onServerRequestInventory(this, requestPlayerInventoryS2CPacket.getId());
        }

        if (packet instanceof SendPlayerInventoryS2CPacket sendPlayerInventoryS2CPacket) {
            eventHandler.onReceiveInventory(this, sendPlayerInventoryS2CPacket.getUsername(), sendPlayerInventoryS2CPacket.getInventory());
        }
    }

    public void doLogin() {
        sendPacket(new LoginC2SPacket(this.username, this.password, this.server, PacketFactory.PROTOCOL_VERSION));
    }

    public void sendPacket(Packet packet) {
        if (socket == null || !socket.isConnected()) throw new IllegalStateException("Socket not open on sendPacket");
        if (eventHandler.onPacketSend(this, packet)) return; // cancelled
        outgoingQueue.offer(packet);
    }

    public String getUsername() {
        return username;
    }

    public void disconnect() {
        if (!this.running) return; // already disconnected
        eventHandler.onDisconnect(this);
        running = false;
        tickExecutor.shutdownNow();

        try {
            if (socket != null && socket.isConnected() && out != null) {
                out.println(PacketFactory.serializePacket(new DisconnectC2SPacket()));
            }
        } catch (Exception ignored) {
        }

        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
