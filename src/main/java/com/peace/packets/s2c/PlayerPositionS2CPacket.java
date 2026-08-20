package com.peace.packets.s2c;

import com.peace.packets.Packet;
import com.peace.packets.PacketId;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCEquipment;
import com.peace.util.IRCNetworkUtils;
import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

@PacketId(0x05)
@SuppressWarnings("unused")
public class PlayerPositionS2CPacket implements Packet {
    private static final int FLAG_HEALTH = 1;
    private static final int FLAG_EQUIPMENT = 1 << 1;

    String username;
    // nullable means BlockPos isn't in render distance (removal when caching)
    @Nullable IRCBlockPos position;
    @Nullable Float health;
    @Nullable IRCEquipment equipment;

    public PlayerPositionS2CPacket(String username, @Nullable IRCBlockPos position) {
        this(username, position, null, null);
    }

    public PlayerPositionS2CPacket(String username, @Nullable IRCBlockPos position, @Nullable Float health, @Nullable IRCEquipment equipment) {
        this.username = username;
        this.position = position;
        this.health = health;
        this.equipment = equipment;
    }

    public PlayerPositionS2CPacket(DataInput in) throws IOException {
        this.username = IRCNetworkUtils.decodeString(in, 1, 20);
        boolean visible = in.readBoolean();
        if (!visible) {
            this.position = null;
            this.health = null;
            this.equipment = null;
            return;
        }

        this.position = new IRCBlockPos(in);

        int flags = in.readUnsignedByte();
        this.health = (flags & FLAG_HEALTH) != 0 ? in.readFloat() : null;
        this.equipment = (flags & FLAG_EQUIPMENT) != 0 ? new IRCEquipment(in) : null;
    }

    public @Nullable IRCBlockPos getPosition() {
        return position;
    }

    public String getUsername() {
        return username;
    }

    public @Nullable Float getHealth() {
        return health;
    }

    public @Nullable IRCEquipment getEquipment() {
        return equipment;
    }

    @Override
    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.username);

        boolean visible = position != null;
        out.writeBoolean(visible);
        if (!visible) return;
        this.position.encode(out);

        int flags = 0;
        if (this.health != null) flags |= FLAG_HEALTH;
        if (this.equipment != null) flags |= FLAG_EQUIPMENT;

        out.writeByte(flags);
        if (this.health != null) out.writeFloat(this.health);
        if (this.equipment != null) this.equipment.encode(out);
    }
}
