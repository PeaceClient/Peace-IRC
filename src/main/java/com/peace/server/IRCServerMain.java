package com.peace.server;

import com.peace.packets.Packet;
import com.peace.packets.s2c.BreakingS2CPacket;
import com.peace.packets.s2c.PlayerPositionS2CPacket;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IRCServerMain {
    public final Map<String, Map<String, IRCServerThread>> serverNameMap = new HashMap<>();
    public final Set<IRCServerThread> notLoggedInSet = new HashSet<>();

    private final IRCServerConfig config;

    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running;

    public IRCServerMain(IRCServerConfig config) {
        this.config = config;
        running = false;
    }

    public void run() throws IOException {
        if (running) return;
        running = true;

        tickExecutor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);

        try (ServerSocket socket = new ServerSocket(config.getPort())) {
            System.out.println("Server up on port: " + config.getPort());

            while (running) {
                Socket clientSocket = socket.accept();

                IRCServerThread thread = new IRCServerThread(clientSocket, this);
                new Thread(thread).start();
            }
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (IRCServerThread notLoggedIn : notLoggedInSet) {
            if (now - notLoggedIn.getConnectMillis() > config.getTimeoutSeconds() * 1000) {
                notLoggedIn.disconnect("Login timeout!");
            }
        }

        for (Map<String, IRCServerThread> server : serverNameMap.values()) {
            for (IRCServerThread player : server.values()) {
                if (player.shouldUpdatePositionAndReset()) {
                    Vec2i pos = player.getPosition();
                    String username = player.getUsername();

                    Packet positionPacket = new PlayerPositionS2CPacket(pos, username);
                    for (IRCServerThread target : server.values()) {
                        if (target != player) {
                            target.sendPacket(positionPacket);
                        }
                    }
                }

                if (player.shouldUpdateBreakingAndReset()) {
                    BlockPos breakPos = player.getBreakingPosition();
                    float progress = player.getBreakingProgress();

                    Packet breakPacket = new BreakingS2CPacket(breakPos, progress, player.getUsername());
                    for (IRCServerThread target : server.values()) {
                        if (target != player) {
                            target.sendPacket(breakPacket);
                        }
                    }
                }
            }
        }
    }

    public void announce(String message) {
        for (Map<String, IRCServerThread> server : serverNameMap.values()) {
            for (IRCServerThread player : server.values()) {
                player.sendServerMessage(message);
            }
        }
    }

    public Map<String, IRCServerThread> getUsers(String server) {
        return serverNameMap.computeIfAbsent(server, (string) -> new HashMap<>());
    }

    public void add(IRCServerThread serverThread) {
        getUsers(serverThread.getServer()).put(serverThread.getUsername(), serverThread);
    }

    public IRCServerConfig getConfig() {
        return this.config;
    }

    public void shutdown() {
        running = false;
        tickExecutor.shutdownNow();
        for (Map<String, IRCServerThread> server : serverNameMap.values()) {
            for (IRCServerThread player : server.values()) {
                player.disconnect("Shutting down");
            }
        }
    }
}
