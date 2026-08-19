package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCNetworkUtils;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x05)
public class PlayerPositionS2CPacket implements Packet {
    String username;
    // nullable means BlockPos isn't in render distance (removal when caching)
    @Nullable IRCBlockPos position;

    public PlayerPositionS2CPacket(String username, @Nullable IRCBlockPos position) {
        this.username = username;
        this.position = position;
    }

    public PlayerPositionS2CPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        boolean visible = in.readBoolean();
        if (!visible) {
            this.position = null;
        } else {
            this.position = new IRCBlockPos(in);
        }
    }

    public @Nullable IRCBlockPos getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);

        boolean visible = position != null;
        out.writeBoolean(visible);
        if (!visible) return;
        this.position.encode(out);
    }
}
