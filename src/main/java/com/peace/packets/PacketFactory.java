package com.peace.packets;

import com.peace.packets.c2s.*;
import com.peace.packets.s2c.*;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketFactory {
    public static final int PROTOCOL_VERSION = 2;
    private static final Map<Byte, Function<DataInput, Packet>> REGISTRY = new HashMap<>();

    static {
        register(BreakingC2SPacket.class);
        register(ChatC2SPacket.class);
        register(DisconnectC2SPacket.class);
        register(LoginC2SPacket.class);
        register(PrivateMessageC2SPacket.class);
        register(RequestPlayerInventoryC2SPacket.class);
        register(SeenEntityC2SPacket.class);
        register(SendPlayerInventoryC2SPacket.class);

        register(BreakingS2CPacket.class);
        register(ChatS2CPacket.class);
        register(DisconnectS2CPacket.class);
        register(IRCUsersS2CPacket.class);
        register(LoginSuccessS2CPacket.class);
        register(PlayerPositionS2CPacket.class);
        register(PrivateMessageS2CPacket.class);
        register(RequestPlayerInventoryS2CPacket.class);
        register(SendPlayerInventoryS2CPacket.class);
        register(ServerMessageS2CPacket.class);
    }

    // TODO: write as records & switch to different encoding for performance
    public static void register(Class<? extends Packet> clazz) {
        PacketId idAnnotation = clazz.getAnnotation(PacketId.class);
        if (idAnnotation == null) {
            throw new IllegalArgumentException("Class missing @PacketId: " + clazz.getName());
        }

        byte packetId = idAnnotation.value();

        try {
            Constructor<? extends Packet> constructor = clazz.getConstructor(DataInput.class);
            REGISTRY.put(packetId, (data) -> {
                try {
                    return constructor.newInstance(data);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate packet: " + clazz.getName(), e);
                }
            });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class missing constructor (DataInput): " + clazz.getName(), e);
        }
    }

    public static Packet createPacket(DataInputStream in) throws IOException {
        byte type = in.readByte();
        Function<DataInput, Packet> factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown packet type: " + type);
        }
        return factory.apply(in);
    }

    public static void serializePacket(DataOutput out, Packet packet) throws IOException {
        byte type = packet.getClass().getAnnotation(PacketId.class).value();
        out.writeByte(type);
        packet.encode(out);
    }
}