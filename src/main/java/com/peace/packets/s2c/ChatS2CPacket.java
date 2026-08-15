package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x0A)
public class ChatS2CPacket implements Packet {
    String username;
    String message;

    public ChatS2CPacket(String username, String message) {
        this.username = username;
        this.message = message;
    }

    public ChatS2CPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        this.message = jsonObject.get("message").getAsString();
    }

    public String getUsername() {
        return username;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("message", this.message);
        return object;
    }
}
