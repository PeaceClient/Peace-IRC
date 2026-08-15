package com.peace.server;

import com.peace.packets.Packet;
import com.peace.packets.s2c.BreakingS2CPacket;
import com.peace.packets.s2c.PlayerPositionS2CPacket;
import com.peace.util.BlockPos;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class IRCServerMain {
    public final Map<String, Map<String, IRCServerThread>> serverNameMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, EntityState>> entityStates = new ConcurrentHashMap<>();

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

        List<PlayerPositionS2CPacket> playerPositions = new ArrayList<>();

        for (Map.Entry<String, Map<String, IRCServerThread>> server : serverNameMap.entrySet()) {
            playerPositions.clear();

            Map<String, EntityState> entityStateMap = getEntityStates(server.getKey());
            Iterator<Map.Entry<String, EntityState>> iterator = entityStateMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, EntityState> entry = iterator.next();
                // timeout for entity lastSeen
                if (now - entry.getValue().millis() > config.getEntityDespawnMillis()) {
                    iterator.remove();
                    // pass null to force removal on clients
                    playerPositions.add(new PlayerPositionS2CPacket(entry.getKey(), null));
                } else {
                    // assume truth and send
                    playerPositions.add(new PlayerPositionS2CPacket(entry.getKey(), entry.getValue().pos()));
                }
            }

            for (IRCServerThread player : server.getValue().values()) {
                if (player.shouldUpdateBreakingAndReset()) {
                    BlockPos breakPos = player.getBreakingPosition();
                    float progress = player.getBreakingProgress();

                    Packet breakPacket = new BreakingS2CPacket(breakPos, progress, player.getUsername());
                    for (IRCServerThread target : server.getValue().values()) {
                        if (target != player) {
                            target.sendPacket(breakPacket);
                        }
                    }
                }

                // TODO: batching for playerpositions!
                for (PlayerPositionS2CPacket packet : playerPositions) {
                    if (packet.getUsername().equals(player.getUsername())) continue;
                    player.sendPacket(packet);
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

    public Map<String, EntityState> getEntityStates(String server) {
        return entityStates.computeIfAbsent(server, (string) -> new HashMap<>());
    }

    public EntityState getEntityState(String server, String username) {
        return getEntityStates(server).get(username);
    }

    public void report(String server, String username, BlockPos pos, long now) {
        getEntityStates(server).put(username, new EntityState(pos, now));
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

    public record EntityState(BlockPos pos, long millis) {
    }
}
