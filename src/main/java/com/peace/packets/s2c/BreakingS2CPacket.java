package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCNetworkUtils;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x08)
public class BreakingS2CPacket implements Packet {
    @Nullable IRCBlockPos position;
    float breakingProgress;
    String username;

    public BreakingS2CPacket(@Nullable IRCBlockPos position, float breakingProgress, String username) {
        this.position = position;
        this.breakingProgress = breakingProgress;
        this.username = username;
    }

    public BreakingS2CPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        boolean breaking = in.readBoolean();
        if (!breaking) {
            this.position = null;
            this.breakingProgress = 0;
        } else {
            this.position = new IRCBlockPos(in);
            this.breakingProgress = in.readFloat();
        }
    }

    public @Nullable IRCBlockPos getPosition() {
        return position;
    }

    public float getBreakingProgress() {
        return breakingProgress;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
        boolean breaking = this.position != null;
        out.writeBoolean(breaking);

        if (!breaking) return;
        this.position.encode(out);
    }
}
