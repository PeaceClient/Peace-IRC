package com.peace.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class IRCInventory {
    private final Map<Integer, IRCItemStack> itemStackMap;

    public IRCInventory(Map<Integer, IRCItemStack> map) {
       this.itemStackMap = map;
    }

    public IRCInventory(JsonObject object) {
        this.itemStackMap = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.asMap().entrySet()) {
            this.itemStackMap.put(Integer.parseInt(entry.getKey()), new IRCItemStack(entry.getValue().getAsJsonObject()));
        }
    }

    public Map<Integer, IRCItemStack> getItemStackMap() {
        return itemStackMap;
    }

    public JsonObject toJson() {
        JsonObject object = new JsonObject();
        for (Map.Entry<Integer, IRCItemStack> entry : this.itemStackMap.entrySet()) {
            object.add(String.valueOf(entry.getKey()), entry.getValue().toJson());
        }
        return object;
    }
}
