package com.peace.client;

import com.peace.packets.Packet;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;

// TODO: move ClientMain into the constructor of ClientEventHandler?
public interface ClientEventHandler {
    // return true cancels event
    boolean onPacketSend(ClientMain main, Packet packet);
    boolean onPacketReceive(ClientMain main, Packet packet);

    void postLogin(ClientMain main);
    // called on self
    void onProgressUpdate(ClientMain main, String username, BlockPos pos, float breakingProgress);
    void onServerMessage(ClientMain main, String message); // handle announces etc
    void onIrcChat(ClientMain main, String sender, String message); // irc chat
    void onPositionReceive(ClientMain main, String username, Vec2i position);
    void tick(ClientMain main);
}
