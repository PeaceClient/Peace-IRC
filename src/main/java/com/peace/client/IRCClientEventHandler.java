package com.peace.client;

import com.peace.packets.Packet;
import com.peace.packets.s2c.IRCUsersS2CPacket;
import com.peace.util.BlockPos;
import org.jspecify.annotations.Nullable;

import java.util.List;

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
    void onPrivateMessage(IRCClientMain main, String sender, String message, boolean isOwnMessage); // irc PM
    default void onIRCUserUpdate(IRCClientMain main, List<String> usernames, IRCUsersS2CPacket.Action action, boolean shouldAnnounce) {}
    default void onPositionReceive(IRCClientMain main, String username, BlockPos position) {
    }

    default void tick(IRCClientMain main) {
    }
}
