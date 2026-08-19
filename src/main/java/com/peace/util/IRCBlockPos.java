package com.peace.util;

public class IRCBlockPos {
    private int x;
    private int y;
    private int z;

    public IRCBlockPos(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
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
