package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x0D)
@SuppressWarnings("unused")
public class PrivateMessageS2CPacket implements Packet {
    String sender;
    String message;
    boolean ownMessage;

    public PrivateMessageS2CPacket(String sender, String message, boolean ownMessage) {
        this.sender = sender;
        this.message = message;
        this.ownMessage = ownMessage;
    }

    public PrivateMessageS2CPacket(DataInput in) throws IOException {
        this.sender = IRCNetworkUtils.decodeString(in, 1, 20);
        this.message = IRCNetworkUtils.decodeString(in, 1, 255);
        this.ownMessage = in.readBoolean();
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOwnMessage() {
        return ownMessage;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.sender);
        IRCNetworkUtils.encodeString(out, this.message);
        out.writeBoolean(this.ownMessage);
    }
}