package com.peace.server;

import com.peace.VersionFeatures;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.s2c.*;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCInventory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class IRCServerMain {
    public final int protocolVersion;
    public final Map<String, Map<String, IRCServerThread>> serverNameMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Map<String, EntityState>> entityStates = new ConcurrentHashMap<>();
    public final Set<IRCServerThread> notLoggedInSet = new HashSet<>();

    private final IRCServerConfig config;

    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean running;
    private final AtomicInteger currentInventoryId;
    private final ConcurrentHashMap<Integer, InventoryRequest> pendingRequests = new ConcurrentHashMap<>();

    public IRCServerMain(IRCServerConfig config) {
        this(config, PacketFactory.PROTOCOL_VERSION); // static constant
    }

    public IRCServerMain(IRCServerConfig config, int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this.config = config;
        running = false;
        currentInventoryId = new AtomicInteger(0);
    }

    public void run() throws IOException {
        if (running) throw new IllegalStateException("Server is already running");
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
        Iterator<IRCServerThread> notLoggedInIt = notLoggedInSet.iterator();
        while (notLoggedInIt.hasNext()) {
            IRCServerThread notLoggedIn = notLoggedInIt.next();
            if (now - notLoggedIn.getConnectMillis() > config.getTimeoutSeconds() * 1000) {
                // dont remove from the list to not crash to concurrency
                notLoggedIn.disconnect("Login timeout!", false);
                // remove
                notLoggedInIt.remove();
            }
        }

        Iterator<Map.Entry<Integer, InventoryRequest>> requestIterator = pendingRequests.entrySet().iterator();
        while (requestIterator.hasNext()) {
            InventoryRequest request = requestIterator.next().getValue();
            if (now - request.startMillis() > config.getRequestTimeoutSeconds() * 1000) {
                request.requester().sendServerMessage("Request timed out!");
                requestIterator.remove();
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
                    IRCBlockPos breakPos = player.getBreakingPosition();
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

    public void sendInventoryRequest(IRCServerThread requester, IRCServerThread target) {
        int id = currentInventoryId.getAndIncrement();
        target.sendPacket(new RequestPlayerInventoryS2CPacket(id));
        pendingRequests.put(id, new InventoryRequest(requester, System.currentTimeMillis()));
    }

    public void fulfillInventoryRequest(IRCServerThread fulfiller, int id, IRCInventory inventory) {
        InventoryRequest request = pendingRequests.get(id);
        if (request == null) {
            fulfiller.sendServerMessage("Invalid id for request!");
            return;
        }

        request.requester().sendPacket(new SendPlayerInventoryS2CPacket(fulfiller.getUsername(), inventory));
        pendingRequests.remove(id);
    }


    public Map<String, IRCServerThread> getUsers(String server) {
        return serverNameMap.computeIfAbsent(server, (string) -> new HashMap<>());
    }

    public void add(IRCServerThread serverThread) {
        getUsers(serverThread.getServer()).put(serverThread.getUsername(), serverThread);
        this.broadcastIRCUser(serverThread.getUsername(), serverThread.getServer(), IRCUsersS2CPacket.Action.Add);
    }

    public void remove(IRCServerThread serverThread) {
        String server = serverThread.getServer();
        String username = serverThread.getUsername();
        this.notLoggedInSet.remove(serverThread);
        if (server != null && username != null) {
            this.getUsers(server).remove(username);
            if (this.getUsers(server).isEmpty()) this.serverNameMap.remove(server); // remove server from nesting
            this.broadcastIRCUser(username, server, IRCUsersS2CPacket.Action.Remove);
        }
    }

    private void broadcastIRCUser(String username, String server, IRCUsersS2CPacket.Action action) {
        for (IRCServerThread player : getUsers(server).values()) {
            if (player.hasFeature(VersionFeatures.IRC_USERS_BROADCASTING)) {
                player.sendPacket(new IRCUsersS2CPacket(List.of(username), action, true));
            }
        }
    }

    public void broadcastAllIRCUsers(IRCServerThread player) {
        if (player.hasFeature(VersionFeatures.IRC_USERS_BROADCASTING)) {
            List<String> playersOnServer = List.copyOf(getUsers(player.getServer()).keySet());
            // don't announce, these are not new players!
            player.sendPacket(new IRCUsersS2CPacket(playersOnServer, IRCUsersS2CPacket.Action.Add, false));
        }
    }

    public Map<String, EntityState> getEntityStates(String server) {
        return entityStates.computeIfAbsent(server, (string) -> new HashMap<>());
    }

    public EntityState getEntityState(String server, String username) {
        return getEntityStates(server).get(username);
    }

    public void report(String server, String username, IRCBlockPos pos, long now) {
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
                player.disconnect("Shutting down", false);
            }
            server.clear();
        }
        serverNameMap.clear();
    }

    public record EntityState(IRCBlockPos pos, long millis) {
    }
    public record InventoryRequest(IRCServerThread requester, long startMillis) {
    }
}
