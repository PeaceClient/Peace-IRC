package com.peace.util;

import com.google.gson.JsonObject;

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

    public IRCBlockPos(JsonObject object) {
        this.x = object.get("x").getAsInt();
        this.y = object.get("y").getAsInt();
        this.z = object.get("z").getAsInt();
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

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("x", this.x);
        object.addProperty("y", this.y);
        object.addProperty("z", this.z);
        return object;
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
