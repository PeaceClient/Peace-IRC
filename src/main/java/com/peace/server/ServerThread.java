package com.peace.server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.*;
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

public class ServerThread implements Runnable {
    private Socket clientSocket;
    private ServerMain serverMain;
    private PrintWriter out;
    private BufferedReader in;


    private boolean loggedIn;
    // TODO: timeout after 10sec if !loggedIn
    private long connectMillis;

    private String playerName;
    private @Nullable Vec2i position;
    // Null when not breaking!
    private @Nullable BlockPos breakingPos;
    private float breakingProgress;


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
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Recieved: " + inputLine);

                try {
                    JsonElement element = JsonParser.parseString(inputLine);

                    JsonObject root = element.getAsJsonObject();

                    Packet packet = PacketFactory.createPacket(root);
                    if (loggedIn) handlePacket(packet);
                    else {
                        if (packet instanceof LoginC2SPacket loginC2SPacket) {
                            // Password is correct!
                            if (Objects.equals(loginC2SPacket.getPassword(), serverMain.password)) {
                                if (serverMain.nameMap.containsKey(this.playerName)) {
                                    System.out.println("Player trying to connect with existing username!");
                                    sendPacket(new LoginS2CPacket(false));
                                    this.disconnect();
                                    return;
                                } else {
                                    System.out.println("Player " + loginC2SPacket.getUsername() + " logged in!");
                                    this.loggedIn = true;
                                    this.playerName = loginC2SPacket.getUsername();
                                    serverMain.nameMap.put(this.playerName, this);

                                    sendPacket(new LoginS2CPacket(true));
                                }
                            }
                        }
                    }
                } catch (JsonSyntaxException | UnsupportedOperationException | IllegalStateException | IllegalArgumentException exception) {
                    System.out.println("Client sent non-valid json!");
                    disconnect();
                    return;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
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
        if (packet instanceof UpdatePositionC2SPacket updatePositionC2SPacket) {
            this.position = updatePositionC2SPacket.getPosition();
            System.out.println("Refreshed player position!");
            return;
        }
        if (packet instanceof BreakingC2SPacket breakingC2SPacket) {
            this.breakingPos = breakingC2SPacket.getPosition();
            this.breakingProgress = breakingC2SPacket.getBreakingProgress();
            System.out.println("Refreshed breaking pos!");
            return;
        }
    }

    public void sendChatMessage(String text) {
        sendPacket(new ServerMessageS2CPacket(text));
    }

    public void sendPacket(Packet packet) {
        if (clientSocket.isConnected() && out != null) {
            out.println(PacketFactory.serializePacket(packet));
        }
    }

    public void disconnect() {
        try {
            serverMain.nameMap.remove(this.playerName);
            if (in != null) in.close();
            if (out != null) out.close();
            in = null;
            out = null;
            if (!clientSocket.isClosed()) clientSocket.close();
            clientSocket = null;
            System.out.println("Closed connection for player: " + (playerName == null ? "None" : playerName));
        } catch (IOException ignored) {
        }
    }
}
