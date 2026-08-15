package com.peace.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ServerMain {
    public final Map<String, ServerThread> nameMap = new HashMap<>();
    protected final String password;
    private final int port;

    public ServerMain(int port, String password) throws IOException {
        this.password = password;
        this.port = port;

        ServerSocket socket = new ServerSocket(port);
        System.out.println("Server up on port: " + port);

        while (true) {
            Socket clientSocket = socket.accept();

            ServerThread thread = new ServerThread(clientSocket, this);
            new Thread(thread).start();
        }
    }
}
