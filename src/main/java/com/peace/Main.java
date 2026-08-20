package com.peace;

import com.peace.client.IRCClientEventHandler;
import com.peace.client.IRCClientMain;
import com.peace.packets.c2s.CustomCallbackC2SPacket;
import com.peace.server.IRCServerConfig;
import com.peace.server.IRCServerMain;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;

import java.io.IOException;

public class Main implements Runnable {
    @CommandLine.Option(names = {"--host", "-h"}, description = "Server hostname")
    private String host = "localhost";

    @CommandLine.Option(names = {"--port", "-p"}, description = "Server port")
    private int port = 8080;

    @CommandLine.Option(names = {"--password"}, required = true, description = "Server password")
    private String password;

    @CommandLine.Option(names = {"--callback", "-c"}, description = "Callback URI, will forward CustomCallbackC2SPacket")
    private String callbackURI;

    @Override
    public void run() {
        // TODO: implement rest of the builder settings!
        IRCServerConfig.Builder builder = new IRCServerConfig.Builder()
                .host(host)
                .port(port)
                .password(password)
                .callbackURI(callbackURI);

        IRCServerMain serverMain = new IRCServerMain(builder.build());

        Thread serverMainThread = new Thread(() -> {
            try {
                serverMain.run();
            } catch (IOException e) {
                e.printStackTrace(System.out);
            }
        });
        serverMainThread.start();


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down!");
            serverMain.shutdown();
            serverMainThread.interrupt();
        }));
    }

    public static void main(String[] args) {
        CommandLine.run(new Main(), System.out, args);
    }
}