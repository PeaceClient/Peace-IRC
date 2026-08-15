package com.peace.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.LoginC2SPacket;
import com.peace.packets.s2c.LoginS2CPacket;
import com.peace.packets.s2c.PlayerPositionS2CPacket;
import com.peace.packets.s2c.ServerMessageS2CPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.*;

public class ClientMain {
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
    private final ClientEventHandler eventHandler;

    private boolean loggedIn;

    public ClientMain(String addr, int port, String username, String password, ClientEventHandler eventHandler) {
        this.addr = addr;
        this.port = port;
        this.username = username;
        this.password = password;
        this.eventHandler = eventHandler;

        this.loggedIn = false;
    }

    public void start() {
        this.running = true;

        try {
            socket = new Socket(addr, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            startReaderThread();

            startWriterThread();

            startTickLoop();

            doLogin();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            try {
                String line;
                while (running && (line = in.readLine()) != null) {
                    try {
                        JsonObject root = JsonParser.parseString(line).getAsJsonObject();
                        Packet packet = PacketFactory.createPacket(root);
                        incomingQueue.offer(packet);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                if (running) e.printStackTrace();
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
                e.printStackTrace();
            }
        }, 0, 50, TimeUnit.MILLISECONDS);
    }

    // TODO: err/info/warn debugs on event handler
    public void handlePacket(Packet packet) {
        if (!loggedIn) {
            if (packet instanceof LoginS2CPacket loginS2CPacket) {
                if (loginS2CPacket.wasSuccessful()) {
                    System.out.println("Logged in!");
                    loggedIn = true;
                } else {
                    System.out.println("Server rejected login!");
                    this.disconnect();
                }
            }
            return;
        }

        if (packet instanceof PlayerPositionS2CPacket playerPositionS2CPacket) {
            eventHandler.onPositionReceive(this, playerPositionS2CPacket.getUsername(), playerPositionS2CPacket.getPosition());
        }

        if (packet instanceof ServerMessageS2CPacket serverMessageS2CPacket) {
            eventHandler.onServerMessage(this, serverMessageS2CPacket.getMessage());
        }
    }

    public void doLogin() {
        sendPacket(new LoginC2SPacket(this.username, this.password));
    }

    public void sendPacket(Packet packet) {
        if (eventHandler.onPacketSend(this, packet)) return; // cancelled
        outgoingQueue.offer(packet);
    }

    public String getUsername() {
        return username;
    }

    public void disconnect() {
        running = false;
        tickExecutor.shutdownNow();
        System.out.println("Client disconnecting!");
        try { if (in != null) in.close(); } catch (IOException ignored) {}
        if (out != null) out.close();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
