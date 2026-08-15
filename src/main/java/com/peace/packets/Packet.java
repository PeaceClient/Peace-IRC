package com.peace.packets;

import com.google.gson.JsonObject;

public interface Packet {
    // Root passed onto constructor and this serializes it
    // NOTE: constructor called from elsewhere in PacketFactory
    JsonObject toJson();
}
