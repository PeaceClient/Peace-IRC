package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.BlockPos;
import org.jspecify.annotations.Nullable;

@PacketId(0x06)
public class BreakingC2SPacket implements Packet {
    @Nullable BlockPos position;
    float breakingProgress;

    // null if not breaking!
    public BreakingC2SPacket(@Nullable BlockPos position, float breakingProgress) {
        this.position = position;
        this.breakingProgress = breakingProgress;
    }

    public BreakingC2SPacket(JsonObject jsonObject) {
        boolean breaking = jsonObject.get("breaking").getAsBoolean();
        if (!breaking) {
            position = null;
            this.breakingProgress = 0;
        } else {
            int x = jsonObject.get("x").getAsInt();
            int y = jsonObject.get("y").getAsInt();
            int z = jsonObject.get("z").getAsInt();
            this.position = new BlockPos(x, y, z);
            this.breakingProgress = jsonObject.get("breakingProgress").getAsFloat();
        }
    }

    public @Nullable BlockPos getPosition() {
        return position;
    }

    public float getBreakingProgress() {
        return breakingProgress;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        if (position == null) {
            object.addProperty("breaking", false);
        } else {
            object.addProperty("breaking", true);
            object.addProperty("x", this.position.getX());
            object.addProperty("y", this.position.getY());
            object.addProperty("z", this.position.getZ());
            object.addProperty("breakingProgress", this.breakingProgress);
        }

        return object;
    }
}

