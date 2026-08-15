package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x07)
public class LoginSuccessS2CPacket implements Packet {

    public LoginSuccessS2CPacket() {}

    public LoginSuccessS2CPacket(JsonObject jsonObject) {}

    @Override
    public JsonObject toJson() {
        return new JsonObject();
    }
}
