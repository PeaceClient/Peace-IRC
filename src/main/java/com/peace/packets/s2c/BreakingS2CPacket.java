package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import org.jspecify.annotations.Nullable;

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

    public BreakingS2CPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        boolean breaking = jsonObject.get("breaking").getAsBoolean();
        if (!breaking) {
            this.position = null;
            this.breakingProgress = 0;
        } else {
            int x = jsonObject.get("x").getAsInt();
            int y = jsonObject.get("y").getAsInt();
            int z = jsonObject.get("z").getAsInt();
            this.breakingProgress = jsonObject.get("breakingProgress").getAsFloat();
            this.position = new IRCBlockPos(x, y, z);
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
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);

        boolean breaking = this.position != null;
        object.addProperty("breaking", breaking);

        if (!breaking) return object;

        object.addProperty("x", this.position.getX());
        object.addProperty("y", this.position.getY());
        object.addProperty("z", this.position.getZ());
        object.addProperty("breakingProgress", this.breakingProgress);

        return object;
    }
}
