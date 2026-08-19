package com.peace.util;

import com.google.gson.JsonObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

// simple data class for an ItemStack
public class IRCItemStack {
    private final String id;
    private final int count;
    private final int damage;
    private final int maxDamage;

    public IRCItemStack(String id, int count, int damage, int maxDamage) {
        this.id = id;
        this.count = count;
        this.damage = damage;
        this.maxDamage = maxDamage;
    }

    public IRCItemStack(DataInput in) throws IOException {
        this.id = IRCNetworkUtils.decodeString(in, 0, 255);
        this.count = in.readInt();
        this.damage = in.readInt();
        this.maxDamage = in.readInt();
    }

    public void encode(DataOutput out) throws IOException {
        IRCNetworkUtils.encodeString(out, this.id);
        out.writeInt(this.count);
        out.writeInt(this.damage);
        out.writeInt(this.maxDamage);
    }

    public String getId() {
        return id;
    }

    public int getCount() {
        return count;
    }

    public int getDamage() {
        return damage;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    @Override
    public String toString() {
        return String.format("IRCItemStack{id:%s,count:%d,damage:%d,maxDamage:%d}", id, count, damage, maxDamage);
    }
}
