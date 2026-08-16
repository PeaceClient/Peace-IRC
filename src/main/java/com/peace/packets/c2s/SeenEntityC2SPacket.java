package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.BlockPos;

@PacketId(0x04)
public class SeenEntityC2SPacket implements Packet {
    String username;
    BlockPos position;

    public SeenEntityC2SPacket(String username, BlockPos position) {
        this.username = username;
        this.position = position;
    }

    public SeenEntityC2SPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();

        int x = jsonObject.get("x").getAsInt();
        int y = jsonObject.get("y").getAsInt();
        int z = jsonObject.get("z").getAsInt();
        this.position = new BlockPos(x, y, z);
    }

    public BlockPos getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);

        object.addProperty("x", this.position.getX());
        object.addProperty("y", this.position.getY());
        object.addProperty("z", this.position.getZ());
        return object;
    }
}
