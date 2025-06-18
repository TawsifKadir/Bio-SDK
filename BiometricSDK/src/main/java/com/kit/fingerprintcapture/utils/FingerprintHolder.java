package com.kit.fingerprintcapture.utils;

public class FingerprintHolder {
    private static Object data;

    public static void set(Object obj) {
        data = obj;
    }

    @SuppressWarnings("unchecked")
    public static <T> T get() {
        return (T) data;
    }

    public static void clear() {
        data = null;
    }
}
