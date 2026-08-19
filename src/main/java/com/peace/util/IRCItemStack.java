package com.peace.util;

import com.google.gson.JsonObject;

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

    public IRCItemStack(JsonObject json) {
        this.id = json.get("id").getAsString();
        this.count = json.get("count").getAsInt();
        this.damage = json.get("damage").getAsInt();
        this.maxDamage = json.get("maxDamage").getAsInt();
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", this.id);
        json.addProperty("count", this.count);
        json.addProperty("damage", this.damage);
        json.addProperty("maxDamage", this.maxDamage);
        return json;
    }

    @Override
    public String toString() {
        return String.format("IRCItemStack{id:%s,count:%d,damage:%d,maxDamage:%d}", id, count, damage, maxDamage);
    }
}
