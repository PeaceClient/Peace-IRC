package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.Vec2i;

@PacketId(0x05)
public class PlayerPositionS2CPacket implements Packet {
    Vec2i position;
    String username;

    public PlayerPositionS2CPacket(Vec2i position, String username) {
        this.position = position;
        this.username = username;
    }

    public PlayerPositionS2CPacket(JsonObject jsonObject) {
        int x = jsonObject.get("x").getAsInt();
        int z = jsonObject.get("z").getAsInt();
        String username = jsonObject.get("username").getAsString();
        this.position = new Vec2i(x, z);
        this.username = username;
    }

    public Vec2i getPosition() {
        return this.position;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("x", this.position.getX());
        object.addProperty("z", this.position.getZ());
        object.addProperty("username", this.username);
        return object;
    }
}
