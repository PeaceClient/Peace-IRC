package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x12)
@SuppressWarnings("unused")
public class CustomCallbackC2SPacket implements Packet {
    private final String data;

    public CustomCallbackC2SPacket(String data) {
        this.data = data;
    }

    public CustomCallbackC2SPacket(DataInput in) throws IOException {
        data = IRCNetworkUtils.decodeString(in);
    }

    public String getData() {
        return data;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.data);
    }
}

