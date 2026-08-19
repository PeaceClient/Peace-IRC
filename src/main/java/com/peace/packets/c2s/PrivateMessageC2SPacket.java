package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x0C)
public class PrivateMessageC2SPacket implements Packet {
    String target;
    String message;

    public PrivateMessageC2SPacket(String target, String message) {
        this.target = target;
        this.message = message;
    }

    public PrivateMessageC2SPacket(DataInput in) throws IOException {
        this.target = IRCNetworkUtils.decodeString(in, 0, 20);
        this.message = IRCNetworkUtils.decodeString(in, 1, 255);
    }

    public String getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.target);
        IRCNetworkUtils.encodeString(out, this.message);
    }
}
