package com.peace.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.peace.packets.Packet;
import com.peace.packets.PacketFactory;
import com.peace.packets.c2s.LoginC2SPacket;
import com.peace.packets.s2c.LoginS2CPacket;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;

public class ClientMain {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

   /* private final BlockingQueue<Packet> incomingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Packet> outgoingQueue = new LinkedBlockingQueue<>();

    private final ScheduledExecutorService tickExecutor = Executors.newSingleThreadScheduledExecutor();

    */

    private final String addr;
    private final int port;
    private final String password;
    private final ClientEventHandler eventHandler;

    private boolean loggedIn;

    public ClientMain(String addr, int port, String password, ClientEventHandler eventHandler) {
        this.addr = addr;
        this.port = port;
        this.password = password;
        this.eventHandler = eventHandler;

        this.loggedIn = false;
    }

    // TODO: err/info/warn debugs on event handler
    public void run() {
        try {
            socket = new Socket(addr, port);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            doLogin();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                try {
                    JsonElement element = JsonParser.parseString(inputLine);

                    JsonObject root = element.getAsJsonObject();

                    Packet packet = PacketFactory.createPacket(root);

                    if (eventHandler.onPacketReceive(this, packet)) continue; // skip packet, cancelled!

                    if (!loggedIn) {
                        if (packet instanceof LoginS2CPacket loginS2CPacket) {
                            if (loginS2CPacket.wasSuccessful()) {
                                System.out.println("Logged in!");
                                loggedIn = true;
                            } else {
                                System.out.println("Server rejected login!");
                                this.disconnect();
                                return;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Error in handling packet, disconnecting!");
                    this.disconnect();
                    return;
                }
            }

        } catch (UnknownHostException e) {
            System.out.println("Unknown host!");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }

    public void doLogin() {
        sendPacket(new LoginC2SPacket("Test", this.password));
    }

    public void sendPacket(Packet packet) {
        if (socket.isConnected() && out != null) {
            boolean shouldCancel = eventHandler.onPacketSend(this, packet);
            if (shouldCancel) return;

            out.println(PacketFactory.serializePacket(packet));
        }
    }

    public void disconnect() {
        System.out.println("Client disconnecting!");
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
    }
}
