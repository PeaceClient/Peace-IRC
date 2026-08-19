package com.peace.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class IRCInventory {
    private final Map<Integer, IRCItemStack> itemStackMap;

    public IRCInventory(Map<Integer, IRCItemStack> map) {
       this.itemStackMap = map;
    }

    public IRCInventory(DataInput in) throws IOException {
        int size = in.readInt();
        if (size > 255) throw new IllegalArgumentException("Size of inventory > 255!");
        this.itemStackMap = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            int slot = in.readInt();
            IRCItemStack stack = new IRCItemStack(in);
            itemStackMap.put(slot, stack);
        }
    }

    public Map<Integer, IRCItemStack> getItemStackMap() {
        return itemStackMap;
    }

    public void encode(DataOutput out) throws IOException {
        out.write(this.itemStackMap.size());
        for (Map.Entry<Integer, IRCItemStack> entry : itemStackMap.entrySet()) {
            out.writeInt(entry.getKey());
            entry.getValue().encode(out);
        }
    }
}
