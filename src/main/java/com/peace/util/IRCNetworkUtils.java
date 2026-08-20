package com.peace.util;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class IRCNetworkUtils {
    public static void encodeString(DataOutput out, String string) throws IOException {
        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    public static String decodeString(DataInput in) throws IOException {
        return decodeString(in, 0, 32767);
    }

    public static String decodeString(DataInput in, int min, int max) throws IOException {
        int len = in.readInt();
        if (len < min || len > max) throw new IllegalArgumentException(String.format("Invalid length for string in range: %d-%d", min, max));
        byte[] stringBytes = new byte[len];
        in.readFully(stringBytes);
        return new String(stringBytes, StandardCharsets.UTF_8);
    }

    public static void encodeStringList(DataOutput out, List<String> list) throws IOException {
        out.writeInt(list.size());
        for (String string : list) {
            encodeString(out, string);
        }
    }

    public static List<String> decodeStringList(DataInput in, int maxSize, int minStrLen, int maxStrLen) throws IOException {
        int size = in.readInt();
        if (size > maxSize) throw new IllegalArgumentException(String.format("String list size larger than expected: %d, expected %d", size, maxSize));

        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String string = decodeString(in, minStrLen, maxStrLen);
            list.add(string);
        }

        return list;
    }
}
