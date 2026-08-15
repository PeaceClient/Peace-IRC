package com.peace.packets;

import com.google.gson.JsonObject;
import com.peace.packets.c2s.*;
import com.peace.packets.s2c.*;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PacketFactory {
    private static final Map<Byte, Function<JsonObject, Packet>> REGISTRY = new HashMap<>();

    static {
        register(BreakingC2SPacket.class);
        register(ChatC2SPacket.class);
        register(DisconnectC2SPacket.class);
        register(LoginC2SPacket.class);
        register(RequestPlayerPositionC2SPacket.class);
        register(PlayerPositionC2SPacket.class);

        register(BreakingS2CPacket.class);
        register(ChatS2CPacket.class);
        register(DisconnectS2CPacket.class);
        register(LoginSuccessS2CPacket.class);
        register(PlayerPositionS2CPacket.class);
        register(ServerMessageS2CPacket.class);
    }

    public static void register(Class<? extends Packet> clazz) {
        PacketId idAnnotation = clazz.getAnnotation(PacketId.class);
        if (idAnnotation == null) {
            throw new IllegalArgumentException("Class missing @PacketId: " + clazz.getName());
        }

        byte packetId = idAnnotation.value();

        try {
            Constructor<? extends Packet> constructor = clazz.getConstructor(JsonObject.class);
            REGISTRY.put(packetId, (data) -> {
                try {
                    return constructor.newInstance(data);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to instantiate packet: " + clazz.getName(), e);
                }
            });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class missing constructor (JsonObject): " + clazz.getName(), e);
        }
    }

    public static Packet createPacket(JsonObject json) {
        byte type = json.get("type").getAsByte();
        JsonObject data = json.getAsJsonObject("data");

        Function<JsonObject, Packet> factory = REGISTRY.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown packet type: " + type);
        }
        return factory.apply(data);
    }

    public static JsonObject serializePacket(Packet packet) {
        byte type = packet.getClass().getAnnotation(PacketId.class).value();

        JsonObject object = new JsonObject();
        object.addProperty("type", type);
        object.add("data", packet.toJson());
        return object;
    }
}