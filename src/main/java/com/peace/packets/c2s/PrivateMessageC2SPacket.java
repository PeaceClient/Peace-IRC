package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x0C)
public class PrivateMessageC2SPacket implements Packet {
    String target;
    String message;

    public PrivateMessageC2SPacket(String target, String message) {
        this.target = target;
        this.message = message;
    }

    public PrivateMessageC2SPacket(JsonObject jsonObject) {
        this.target = jsonObject.get("target").getAsString();
        this.message = jsonObject.get("message").getAsString();
    }

    public String getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("target", this.target);
        object.addProperty("message", this.message);
        return object;
    }
}
