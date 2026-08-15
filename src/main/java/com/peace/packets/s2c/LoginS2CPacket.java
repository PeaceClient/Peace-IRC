package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x07)
public class LoginS2CPacket implements Packet {
    boolean success;

    public LoginS2CPacket(boolean success) {
        this.success = success;
    }

    public LoginS2CPacket(JsonObject jsonObject) {
        this.success = jsonObject.get("success").getAsBoolean();
    }

    public boolean wasSuccessful() {
        return success;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("success", this.success);
        return object;
    }
}
