package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x0e)
public class RequestPlayerInventoryS2CPacket implements Packet {
    // Not going to include username, not a privacy issue in a normal use case
    int id;

    public RequestPlayerInventoryS2CPacket(int id) {
        this.id = id;
    }

    public RequestPlayerInventoryS2CPacket(DataInput in) throws IOException {
        this.id = in.readInt();
    }

    public int getId() {
        return id;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        out.writeInt(this.id);
    }
}
