package com.peace;

import com.peace.client.ClientEventHandler;
import com.peace.client.ClientMain;
import com.peace.packets.Packet;
import com.peace.packets.c2s.PlayerPositionC2SPacket;
import com.peace.packets.s2c.PlayerPositionS2CPacket;
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
    public void onServerMessage(ClientMain main, String message) {
        System.out.println("Chat message: " + message);
    }

    @Override
    public void onIrcChat(ClientMain main, String sender, String message) {
        System.out.println("IRC message: " + sender + " - " + message);
    }

    @Override
    public void onPositionReceive(ClientMain main, String username, Vec2i position) {
        System.out.println("Received position by user: " + username + " at " + position + " at client: " + main.getUsername());
    }

    int tick = 0;
    @Override
    public void tick(ClientMain main) {
        tick++;
        if (tick == 60) {
            if (main.getUsername().equals("Player1")) main.sendPacket(new PlayerPositionC2SPacket(new Vec2i(100, 100)));
        }
    }
}