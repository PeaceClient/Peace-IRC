package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x03)
public class RequestPlayerInventoryC2SPacket implements Packet {
    String username;

    public RequestPlayerInventoryC2SPacket(String username) {
        this.username = username;
    }

    public RequestPlayerInventoryC2SPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
    }
}
