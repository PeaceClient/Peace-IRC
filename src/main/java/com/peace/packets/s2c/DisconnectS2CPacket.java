package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x0f)
@SuppressWarnings("unused")
public class DisconnectS2CPacket implements Packet {
    String reason;

    public DisconnectS2CPacket(String reason) {
        this.reason = reason;
    }

    public DisconnectS2CPacket(DataInput in) throws IOException {
        this.reason = IRCNetworkUtils.decodeString(in, 0, 255);
    }

    public String getReason() {
        return reason;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.reason);
    }
}

