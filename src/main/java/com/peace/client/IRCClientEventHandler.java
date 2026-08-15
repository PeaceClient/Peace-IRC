package com.peace.client;

import com.peace.packets.Packet;
import com.peace.util.BlockPos;
import com.peace.util.Vec2i;
import org.jspecify.annotations.Nullable;

// TODO: move ClientMain into the constructor of ClientEventHandler?
public interface IRCClientEventHandler {
    // return true cancels event
    default boolean onPacketSend(IRCClientMain main, Packet packet) {return false;}
    default boolean onPacketReceive(IRCClientMain main, Packet packet) {return false;};

    default void postLogin(IRCClientMain main) {}
    default void onDisconnect(IRCClientMain main) {}
    default void onKick(IRCClientMain main, String reason) {}
    default void onProgressUpdate(IRCClientMain main, String username, @Nullable BlockPos pos, float breakingProgress) {}
    void onServerMessage(IRCClientMain main, String message); // handle announces etc
    void onIrcChat(IRCClientMain main, String sender, String message); // irc chat
    default void onPositionReceive(IRCClientMain main, String username, Vec2i position) {}
    void tick(IRCClientMain main);
}
