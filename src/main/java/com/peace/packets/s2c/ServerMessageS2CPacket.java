package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x02)
public class ServerMessageS2CPacket implements Packet {
    String message;

    public ServerMessageS2CPacket(String message) {
        this.message = message;
    }

    public ServerMessageS2CPacket(JsonObject jsonObject) {
        this.message = jsonObject.get("message").getAsString();
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("message", this.message);
        return object;
    }
}
