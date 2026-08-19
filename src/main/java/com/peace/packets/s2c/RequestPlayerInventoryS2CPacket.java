package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x0e)
public class RequestPlayerInventoryS2CPacket implements Packet {
    // Not going to include username, not a privacy issue in a normal use case
    int id;

    public RequestPlayerInventoryS2CPacket(int id) {
        this.id = id;
    }

    public RequestPlayerInventoryS2CPacket(JsonObject jsonObject) {
        this.id = jsonObject.get("id").getAsInt();
    }

    public int getId() {
        return id;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", this.id);
        return object;
    }
}
