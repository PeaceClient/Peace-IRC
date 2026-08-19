package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x07)
public class LoginSuccessS2CPacket implements Packet {

    public LoginSuccessS2CPacket() {}

    public LoginSuccessS2CPacket(DataInput in) {}
    @Override
    public void encode(DataOutput out) throws IOException {}
}
