package com.peace.packets.s2c;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x0D)
public class PrivateMessageS2CPacket implements Packet {
    String sender;
    String message;
    boolean ownMessage;

    public PrivateMessageS2CPacket(String sender, String message, boolean ownMessage) {
        this.sender = sender;
        this.message = message;
        this.ownMessage = ownMessage;
    }

    public PrivateMessageS2CPacket(JsonObject jsonObject) {
        this.sender = jsonObject.get("sender").getAsString();
        this.message = jsonObject.get("message").getAsString();
        this.ownMessage = jsonObject.get("ownMessage").getAsBoolean();
    }

    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOwnMessage() {
        return ownMessage;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("sender", this.sender);
        object.addProperty("message", this.message);
        object.addProperty("ownMessage", this.ownMessage);
        return object;
    }
}
