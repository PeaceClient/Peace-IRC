package com.peace.packets.s2c;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import javax.xml.crypto.Data;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public IRCUsersS2CPacket(DataInput in) throws IOException {
        this.usernames = IRCNetworkUtils.decodeStringList(in, 256, 1, 20);
        this.action = Action.values()[in.readInt()];
        this.announce = in.readBoolean();
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
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeStringList(out, this.usernames);
        out.writeInt(this.action.ordinal());
        out.writeBoolean(this.announce);
    }

    public enum Action {
        Add,
        Remove
    }
}


