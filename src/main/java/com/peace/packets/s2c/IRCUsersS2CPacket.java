package com.peace.packets.s2c;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

import java.util.ArrayList;
import java.util.List;
/*
@PacketId(0x0B)
public class IRCUsersS2CPacket implements Packet {
    List<String> usernames;
    Action action;
    boolean announce;

    public IRCUsersS2CPacket(List<String> usernames, Action action, boolean announce) {
        this.usernames = usernames;
        this.action = action;
        this.announce = announce;
    }

    public IRCUsersS2CPacket(JsonObject jsonObject) {
        usernames = new ArrayList<>();
        for (JsonElement element : jsonObject.get("usernames").getAsJsonArray()) {
            usernames.add(element.getAsString());
        }

        action = Action.values()[jsonObject.get("action").getAsShort()];
        announce = jsonObject.get("announce").getAsBoolean();
    }

    public List<String> getUsernames() {
        return usernames;
    }

    public Action getAction() {
        return action;
    }

    public boolean shouldAnnounce() {
        return announce;
    }

    @Override
    public JsonObject encode() {
        JsonObject object = new JsonObject();

        JsonArray userArray = new JsonArray();
        for (String username : this.usernames) {
            userArray.add(username);
        }

        object.add("usernames", userArray);
        object.addProperty("action", this.action.ordinal());
        object.addProperty("announce", this.announce);
        return object;
    }

    public enum Action {
        Add,
        Remove
    }
}

 */
