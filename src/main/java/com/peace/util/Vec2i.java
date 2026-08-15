package com.peace.util;

public class Vec2i {
    private int x;
    private int z;

    public Vec2i(int x, int z) {
        this.x = x;
        this.z = z;
    }


    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return String.format("{%d, %d}", getX(), getZ());
    }
}
