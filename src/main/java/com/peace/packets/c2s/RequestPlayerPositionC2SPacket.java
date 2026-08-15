package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x03)
public class RequestPlayerPositionC2SPacket implements Packet {
    String username;

    public RequestPlayerPositionC2SPacket(String username) {
        this.username = username;
    }

    public RequestPlayerPositionC2SPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
    }

    public String getUsername() {
        return this.username;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        return object;
    }
}
