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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerMain {
    public final Map<String, ServerThread> nameMap = new HashMap<>();

    private final ServerConfig config;

    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    public ServerMain(ServerConfig config) throws IOException {
        this.config = config;

        tickExecutor.scheduleAtFixedRate(this::tick, 0, 50, TimeUnit.MILLISECONDS);

        ServerSocket socket = new ServerSocket(config.getPort());
        System.out.println("Server up on port: " + config.getPort());

        while (true) {
            Socket clientSocket = socket.accept();

            ServerThread thread = new ServerThread(clientSocket, this);
            new Thread(thread).start();
        }
    }

    private void tick() {
        for (ServerThread player : nameMap.values()) {
            if (player.shouldUpdatePositionAndReset()) {
                System.out.println("Update position for player " + player.getUsername());
                Vec2i pos = player.getPosition();
                String username = player.getUsername();

                Packet positionPacket = new PlayerPositionS2CPacket(pos, username);
                for (ServerThread target : nameMap.values()) {
                    if (target != player) {
                        target.sendPacket(positionPacket);
                    }
                }
            }

            if (player.shouldUpdateBreakingAndReset()) {
                BlockPos breakPos = player.getBreakingPosition();
                float progress = player.getBreakingProgress();

                Packet breakPacket = new BreakingS2CPacket(breakPos, progress, player.getUsername());
                for (ServerThread target : nameMap.values()) {
                    if (target != player) {
                        target.sendPacket(breakPacket);
                    }
                }
            }
        }
    }

    public ServerConfig getConfig() {
        return this.config;
    }

    public void shutdown() {
        tickExecutor.shutdownNow();
        for (ServerThread t : nameMap.values()) {
            t.disconnect();
        }
    }
}
