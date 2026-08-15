package com.peace;

import com.peace.client.ClientMain;
import com.peace.server.ServerMain;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        Thread serverMainThread = new Thread(() -> {
            try {
                new ServerMain(8080, "TestPassword");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverMainThread.start();

        ClientMain clientMain = new ClientMain("localhost", 8080, "TestPassword", new DebugEventHandler());
        Thread clientTestThread = new Thread(clientMain::run);
        clientTestThread.start();
    }
}