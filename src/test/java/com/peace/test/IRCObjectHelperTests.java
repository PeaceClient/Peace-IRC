package com.peace.test;

import com.peace.packets.PacketFactory;
import com.peace.util.IRCBlockPos;
import com.peace.util.IRCItemStack;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class IRCObjectHelperTests {
    @Test
    void testBlockPos() {
        // equality
        IRCBlockPos first = new IRCBlockPos(1,2,3);
        IRCBlockPos second = new IRCBlockPos(1,2,3);
        assertEquals(first, second);
        assertNotEquals(null, first);
        assertNotEquals(new IRCBlockPos(0,0,0), first);

        // no bytes left, throws
        assertThrows(EOFException.class, () -> new IRCBlockPos(new DataInputStream(new ByteArrayInputStream(new byte[0]))));

        // encode/decode equality
        byte[] firstEncoded = encode(first::encode);
        IRCBlockPos firstDecoded = decode(IRCBlockPos::new, firstEncoded);

        assertEquals(first, firstDecoded);
    }

    @Test
    void testItemStack() {
        IRCItemStack fireStack = new IRCItemStack("fire", 1, 0, 0);
        IRCItemStack waterStack = new IRCItemStack("water", 5, 0, 6);

        assertEquals(1, fireStack.getCount());
        assertEquals(6, waterStack.getMaxDamage());
        assertEquals("fire", fireStack.getId());
        assertEquals(0, waterStack.getDamage());

        byte[] fireEncoded = encode(fireStack::encode);
        IRCItemStack decoded = decode(IRCItemStack::new, fireEncoded);
        assertEquals(fireStack, decoded);
    }


    private byte[] encode(ThrowableConsumer<DataOutput, IOException> encode) {
        try {
            ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
            DataOutputStream tmpOut = new DataOutputStream(byteArrayStream);

            encode.accept(tmpOut);

            tmpOut.flush();

            return byteArrayStream.toByteArray();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private <T> T decode(ThrowableFunction<DataInput, T, IOException> decode, byte[] payload) {
        try {
            DataInputStream payloadIn = new DataInputStream(new ByteArrayInputStream(payload));
            return decode.apply(payloadIn);
        } catch (IOException e) {
            return null;
        }
    }
}
