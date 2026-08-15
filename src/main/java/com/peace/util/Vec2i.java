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

    public double squaredDistanceTo(Vec2i pos) {
        return (pos.getX()*pos.getX() - this.getX()*this.getX()) + (pos.getZ()*pos.getZ() - this.getZ()*this.getZ());
    }

    @Override
    public String toString() {
        return String.format("{%d, %d}", getX(), getZ());
    }
}
