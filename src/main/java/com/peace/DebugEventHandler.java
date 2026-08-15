package com.peace;

import com.peace.client.ClientEventHandler;
import com.peace.client.ClientMain;
import com.peace.packets.Packet;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;

public class DebugEventHandler implements ClientEventHandler {

    @Override
    public boolean onPacketSend(ClientMain main, Packet packet) {
        return false;
    }

    @Override
    public boolean onPacketReceive(ClientMain main, Packet packet) {
        return false;
    }

    @Override
    public void postLogin(ClientMain main) {
        System.out.println("Logged in");
    }

    @Override
    public void onProgressUpdate(ClientMain main, String username, BlockPos pos, float breakingProgress) {
        System.out.println("Progress update");
    }

    @Override
    public void onChatMessage(ClientMain main, String message) {
        System.out.println("Chat message: " + message);
    }

    @Override
    public void onIrcChat(ClientMain main, String sender, String message) {
        System.out.println("IRC message: " + sender + " - " + message);
    }

    @Override
    public void onPositionReceive(ClientMain main, String username, Vec2i position) {
        System.out.println("position by user: " + username + " at " + position);
    }

    @Override
    public void tick(ClientMain main) {

    }
}