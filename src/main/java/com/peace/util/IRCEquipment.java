package com.peace.util;

import org.jspecify.annotations.Nullable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class IRCEquipment {
    private static final int SLOT_COUNT = 6;

    private static final int MAIN_HAND = 0;
    private static final int OFF_HAND = 1;
    private static final int HELMET = 2;
    private static final int CHESTPLATE = 3;
    private static final int LEGGINGS = 4;
    private static final int BOOTS = 5;

    private final @Nullable IRCItemStack[] slots;

    public IRCEquipment(@Nullable IRCItemStack mainHand, @Nullable IRCItemStack offHand, @Nullable IRCItemStack helmet,
                        @Nullable IRCItemStack chestplate, @Nullable IRCItemStack leggings, @Nullable IRCItemStack boots) {
        this.slots = new IRCItemStack[]{mainHand, offHand, helmet, chestplate, leggings, boots};
    }

    public IRCEquipment(DataInput in) throws IOException {
        int mask = in.readUnsignedByte();
        this.slots = new IRCItemStack[SLOT_COUNT];
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if ((mask & (1 << slot)) != 0) {
                this.slots[slot] = new IRCItemStack(in);
            }
        }
    }

    public void encode(DataOutput out) throws IOException {
        int mask = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (this.slots[slot] != null) mask |= 1 << slot;
        }

        out.writeByte(mask);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            IRCItemStack stack = this.slots[slot];
            if (stack != null) stack.encode(out);
        }
    }

    public boolean isEmpty() {
        for (IRCItemStack stack : this.slots) {
            if (stack != null) return false;
        }
        return true;
    }

    public @Nullable IRCItemStack getMainHand() {
        return this.slots[MAIN_HAND];
    }

    public @Nullable IRCItemStack getOffHand() {
        return this.slots[OFF_HAND];
    }

    public @Nullable IRCItemStack getHelmet() {
        return this.slots[HELMET];
    }

    public @Nullable IRCItemStack getChestplate() {
        return this.slots[CHESTPLATE];
    }

    public @Nullable IRCItemStack getLeggings() {
        return this.slots[LEGGINGS];
    }

    public @Nullable IRCItemStack getBoots() {
        return this.slots[BOOTS];
    }

    @Override
    public String toString() {
        return String.format("IRCEquipment{mainHand:%s,offHand:%s,helmet:%s,chestplate:%s,leggings:%s,boots:%s}",
                getMainHand(), getOffHand(), getHelmet(), getChestplate(), getLeggings(), getBoots());
    }
}
