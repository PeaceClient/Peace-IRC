package com.peace.packets.c2s;


import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCInventory;

@PacketId(0x10)
public class SendPlayerInventoryC2SPacket implements Packet {
    int id;
    IRCInventory inventory;

    public SendPlayerInventoryC2SPacket(int id, IRCInventory inventory) {
        this.id = id;
        this.inventory = inventory;
    }

    public SendPlayerInventoryC2SPacket(JsonObject jsonObject) {
        this.id = jsonObject.get("id").getAsInt();
        this.inventory = new IRCInventory(jsonObject.get("inventory").getAsJsonObject());
    }

    public int getId() {
        return id;
    }

    public IRCInventory getInventory() {
        return inventory;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("id", this.id);
        object.add("inventory", this.inventory.toJson());
        return object;
    }
}

