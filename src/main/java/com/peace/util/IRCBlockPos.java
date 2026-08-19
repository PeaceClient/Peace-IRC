package com.peace.util;

import com.google.gson.JsonObject;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;

public class IRCBlockPos {
    private int x;
    private int y;
    private int z;

    public IRCBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public IRCBlockPos(DataInput in) throws IOException {
        this.x = in.readInt();
        this.y = in.readInt();
        this.z = in.readInt();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public double horizontalSquaredDistanceTo(IRCBlockPos pos) {
        double x = this.getX() - pos.getX();
        double z = this.getZ() - pos.getZ();
        return x * x + z * z;
    }

    public void encode(DataOutput out) throws IOException {
        out.writeInt(this.x);
        out.writeInt(this.y);
        out.writeInt(this.z);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IRCBlockPos blockPos)) return false;
        return getX() == blockPos.getX() && getY() == blockPos.getY() && getZ() == blockPos.getZ();
    }

    @Override
    public String toString() {
        return String.format("{%d, %d, %d}", getX(), getY(), getZ());
    }
}
