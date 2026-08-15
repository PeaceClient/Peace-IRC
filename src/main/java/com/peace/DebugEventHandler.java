package com.peace;

import com.peace.client.IRCClientEventHandler;
import com.peace.client.IRCClientMain;
import com.peace.packets.c2s.ChatC2SPacket;
import com.peace.packets.c2s.PlayerPositionC2SPacket;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;

public class DebugEventHandler implements IRCClientEventHandler {

    @Override
    public void postLogin(IRCClientMain main) {
        System.out.println("Called from post-login!");
    }

    @Override
    public void onProgressUpdate(IRCClientMain main, String username, BlockPos pos, float breakingProgress) {
        System.out.println("Progress update at pos: " + pos + " with prog " + breakingProgress);
    }

    @Override
    public void onServerMessage(IRCClientMain main, String message) {
        System.out.println("Server message (current client: " + main.getUsername() + "): " + message);
    }

    @Override
    public void onIrcChat(IRCClientMain main, String sender, String message) {
        System.out.println("IRC message (current client: " + main.getUsername() + "): " + sender + " - " + message);
    }

    int tick = 0;
    @Override
    public void tick(IRCClientMain main) {
        tick++;
        if (tick == 60) {
            if (main.getUsername().equals("Player1")) main.sendPacket(new PlayerPositionC2SPacket(new Vec2i(100, 100)));
        }

        if (tick == 100) {
            if (main.getUsername().equals("Player2")) main.sendPacket(new ChatC2SPacket("hi"));
            if (main.getUsername().equals("Player2")) main.sendPacket(new ChatC2SPacket("hi"));
        }
    }
}