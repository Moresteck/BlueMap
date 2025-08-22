package de.bluecolored.bluemap.core.util;

import java.util.concurrent.ConcurrentHashMap;

public class StringPool {
    private static final ConcurrentHashMap<String, String> STRING_POOL = new ConcurrentHashMap<>();

    public static String intern(String str) {
        return STRING_POOL.computeIfAbsent(str, key -> key);
    }
}