package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x0A)
@SuppressWarnings("unused")
public class ChatS2CPacket implements Packet {
    String username;
    String message;

    public ChatS2CPacket(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public ChatS2CPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        this.message = IRCNetworkUtils.decodeString(in, 1, 255);
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
        IRCNetworkUtils.encodeString(out, this.message);
    }
}
