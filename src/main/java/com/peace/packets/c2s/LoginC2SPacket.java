package com.peace.packets.c2s;

import com.google.gson.JsonObject;
import com.peace.packets.Packet;
import com.peace.packets.PacketId;

@PacketId(0x01)
public class LoginC2SPacket implements Packet {
    String username;
    String password;
    String server;
    int protocolVersion;

    public LoginC2SPacket(String username, String password, String server, int protocolVersion) {
        this.username = username;
        this.password = password;
        this.server = server;
        this.protocolVersion = protocolVersion;
    }

    public LoginC2SPacket(JsonObject jsonObject) {
        this.username = jsonObject.get("username").getAsString();
        this.password = jsonObject.get("password").getAsString();
        this.server = jsonObject.get("server").getAsString();
        // LEGACY PARSING
        if (jsonObject.has("protocolVersion")) this.protocolVersion = jsonObject.get("protocolVersion").getAsInt();
        else protocolVersion = 0;
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getServer() {
        return server;
    }

    public int getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("username", this.username);
        object.addProperty("password", this.password);
        object.addProperty("server", this.server);
        object.addProperty("protocolVersion", this.protocolVersion);
        return object;
    }
}
