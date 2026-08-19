package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCInventory;

@PacketId(0x11)
public class SendPlayerInventoryS2CPacket implements Packet {
    String username;
    IRCInventory inventory;

    public SendPlayerInventoryS2CPacket(String username, IRCInventory inventory) {
        this.username = username;
        this.inventory = inventory;
    }

    public SendPlayerInventoryS2CPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        this.inventory = new IRCInventory(jsonObject.get("inventory").getAsJsonObject());
    }

    public String getUsername() {
        return username;
    }

    public IRCInventory getInventory() {
        return inventory;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.add("inventory", this.inventory.toJson());
        return object;
    }
}
