package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x01)
public class LoginC2SPacket implements Packet {
    String username;
    String password;

    public LoginC2SPacket(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public LoginC2SPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        this.password = jsonObject.get("password").getAsString();
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("password", this.password);
        return object;
    }
}
