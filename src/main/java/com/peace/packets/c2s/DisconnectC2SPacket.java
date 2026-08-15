package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x00)
public class DisconnectC2SPacket implements Packet {
    public DisconnectC2SPacket() {}

    public DisconnectC2SPacket(JsonObject jsonObject) {
    }

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }
}

