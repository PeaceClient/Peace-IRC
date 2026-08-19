package com.peace.packets.c2s;


import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCInventory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x10)
public class SendPlayerInventoryC2SPacket implements Packet {
    int id;
    IRCInventory inventory;

    public SendPlayerInventoryC2SPacket(int id, IRCInventory inventory) {
        this.id = id;
        this.inventory = inventory;
    }

    public SendPlayerInventoryC2SPacket(DataInput in) throws IOException {
        this.id = in.readInt();
        this.inventory = new IRCInventory(in);
    }

    public int getId() {
        return id;
    }

    public IRCInventory getInventory() {
        return inventory;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        out.writeInt(id);
        this.inventory.encode(out);
    }
}

