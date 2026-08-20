package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x02)
@SuppressWarnings("unused")
public class ServerMessageS2CPacket implements Packet {
    String message;

    public ServerMessageS2CPacket(String message) {
        this.message = message;
    }

    public ServerMessageS2CPacket(DataInput in) throws IOException {
        this.message = IRCNetworkUtils.decodeString(in, 1, 255);
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.message);
    }
}
