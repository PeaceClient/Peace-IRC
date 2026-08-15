package com.peace;

import com.peace.server.IRCServerConfig;
import com.peace.server.IRCServerMain;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        IRCServerConfig config = new IRCServerConfig.Builder()
                .host("localhost")
                .port(8080)
                .password("TestPassword")
                .build();

        IRCServerMain serverMain = new IRCServerMain(config);

        Thread serverMainThread = new Thread(() -> {
            try {
                serverMain.run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverMainThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            serverMain.shutdown();
            try {
                serverMainThread.join();
            } catch (InterruptedException ignored) {
            }
        }));
    }
}