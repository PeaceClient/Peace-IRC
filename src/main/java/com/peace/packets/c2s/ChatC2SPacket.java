package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x09)
public class ChatC2SPacket implements Packet {
    String message;

    public ChatC2SPacket(String message) {
        this.message = message;
    }

    public ChatC2SPacket(JsonObject jsonObject) {
        this.message = jsonObject.get("message").getAsString();
    }

    public String getMessage() {
        return message;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("message", this.message);
        return object;
    }
}
