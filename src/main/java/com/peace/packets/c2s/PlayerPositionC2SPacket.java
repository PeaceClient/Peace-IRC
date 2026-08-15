package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.Vec2i;

@PacketId(0x04)
public class PlayerPositionC2SPacket implements Packet {
    Vec2i position;

    public PlayerPositionC2SPacket(Vec2i position) {
        this.position = position;
    }

    public PlayerPositionC2SPacket(JsonObject jsonObject) {
        int x = jsonObject.get("x").getAsInt();
        int z = jsonObject.get("z").getAsInt();
        this.position = new Vec2i(x, z);
    }

    public Vec2i getPosition() {
        return this.position;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("x", this.position.getX());
        object.addProperty("z", this.position.getZ());
        return object;
    }
}
