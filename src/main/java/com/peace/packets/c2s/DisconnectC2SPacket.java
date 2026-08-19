package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x00)
public class DisconnectC2SPacket implements Packet {
    public DisconnectC2SPacket() {}

    public DisconnectC2SPacket(DataInput in) {}

    @Override
    public void encode(DataOutput out) throws IOException {}
}

