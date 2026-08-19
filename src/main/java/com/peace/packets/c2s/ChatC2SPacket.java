package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x09)
public class ChatC2SPacket implements Packet {
    private final String message;

    public ChatC2SPacket(String message) {
        this.message = message;
    }

    public ChatC2SPacket(DataInput in) throws IOException {
        message = IRCNetworkUtils.decodeString(in, 1, 255);
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.message);
    }
}
