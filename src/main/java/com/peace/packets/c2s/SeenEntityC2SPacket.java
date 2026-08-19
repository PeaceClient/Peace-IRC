package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x04)
public class SeenEntityC2SPacket implements Packet {
    String username;
    IRCBlockPos position;

    public SeenEntityC2SPacket(String username, IRCBlockPos position) {
        this.username = username;
        this.position = position;
    }

    public SeenEntityC2SPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        this.position = new IRCBlockPos(in);
    }

    public IRCBlockPos getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
        this.position.encode(out);
    }
}
