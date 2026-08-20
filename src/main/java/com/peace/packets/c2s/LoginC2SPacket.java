package com.peace.packets.c2s;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCNetworkUtils;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x01)
@SuppressWarnings("unused")
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

    public LoginC2SPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        this.password = IRCNetworkUtils.decodeString(in, 0, 100);
        this.server = IRCNetworkUtils.decodeString(in, 1, 200);
        this.protocolVersion = in.readInt();
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
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);
        IRCNetworkUtils.encodeString(out, this.password);
        IRCNetworkUtils.encodeString(out, this.server);
        out.writeInt(this.protocolVersion);
    }
}
