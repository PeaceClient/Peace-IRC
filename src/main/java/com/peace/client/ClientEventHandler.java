package com.peace.client;

import com.peace.packets.Packet;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;
import org.jspecify.annotations.Nullable;

// TODO: move ClientMain into the constructor of ClientEventHandler?
public interface ClientEventHandler {
    // return true cancels event
    default boolean onPacketSend(ClientMain main, Packet packet) {return false;}
    default boolean onPacketReceive(ClientMain main, Packet packet) {return false;};

    default void postLogin(ClientMain main) {};
    default void onProgressUpdate(ClientMain main, String username, @Nullable BlockPos pos, float breakingProgress) {};
    void onServerMessage(ClientMain main, String message); // handle announces etc
    void onIrcChat(ClientMain main, String sender, String message); // irc chat
    default void onPositionReceive(ClientMain main, String username, Vec2i position) {};
    void tick(ClientMain main);
}
