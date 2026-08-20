package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x06)
@SuppressWarnings("unused")
public class BreakingC2SPacket implements Packet {
    @Nullable IRCBlockPos position;
    float breakingProgress;

    // null if not breaking!
    public BreakingC2SPacket(@Nullable IRCBlockPos position, float breakingProgress) {
        this.position = position;
        this.breakingProgress = breakingProgress;
    }

    public BreakingC2SPacket(DataInput in) throws IOException {
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

    @Override
    public void encode(DataOutput out) throws IOException {
        boolean breaking = this.position != null;

        out.writeBoolean(breaking);
        // null?
        if (!breaking) return;

        this.position.encode(out);
        out.writeFloat(this.breakingProgress);
    }
}

