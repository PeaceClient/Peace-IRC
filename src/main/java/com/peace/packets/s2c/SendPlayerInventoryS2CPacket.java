package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCInventory;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x11)
public class SendPlayerInventoryS2CPacket implements Packet {
    String username;
    IRCInventory inventory;

    public SendPlayerInventoryS2CPacket(String username, IRCInventory inventory) {
        this.username = username;
        this.inventory = inventory;
    }

    public SendPlayerInventoryS2CPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        this.inventory = new IRCInventory(in);
    }

    public String getUsername() {
        return username;
    }

    public IRCInventory getInventory() {
        return inventory;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
        this.inventory.encode(out);
    }
}