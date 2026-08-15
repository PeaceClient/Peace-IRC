package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x0f)
public class DisconnectS2CPacket implements Packet {
    String reason;

    public DisconnectS2CPacket(String reason) {
        this.reason = reason;
    }

    public DisconnectS2CPacket(JsonObject jsonObject) {
        this.reason = jsonObject.get("reason").getAsString();
    }

    public String getReason() {
        return reason;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("reason", this.reason);
        return object;
    }
}

