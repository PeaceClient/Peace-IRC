package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import org.jspecify.annotations.Nullable;

@PacketId(0x05)
public class PlayerPositionS2CPacket implements Packet {
    String username;
    // nullable means BlockPos isn't in render distance (removal when caching)
    @Nullable IRCBlockPos position;

    public PlayerPositionS2CPacket(String username, @Nullable IRCBlockPos position) {
        this.username = username;
        this.position = position;
    }

    public PlayerPositionS2CPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        boolean visible = jsonObject.get("visible").getAsBoolean();
        if (!visible) {
            this.position = null;
        } else {
            int x = jsonObject.get("x").getAsInt();
            int y = jsonObject.get("y").getAsInt();
            int z = jsonObject.get("z").getAsInt();
            this.position = new IRCBlockPos(x, y, z);
        }
    }

    public @Nullable IRCBlockPos getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);

        boolean visible = position != null;
        object.addProperty("visible", visible);

        if (!visible) return object;

        object.addProperty("x", this.position.getX());
        object.addProperty("y", this.position.getY());
        object.addProperty("z", this.position.getZ());
        return object;
    }
}
