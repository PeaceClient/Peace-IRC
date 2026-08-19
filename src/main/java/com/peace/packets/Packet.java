package com.peace.packets;

import java.io.DataOutput;
import java.io.IOException;

public interface Packet {
    // Root passed onto constructor and this serializes it
    // NOTE: constructor called from elsewhere in PacketFactory
    void encode(DataOutput out) throws IOException;
}
