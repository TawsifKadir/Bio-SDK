package com.kit.fingerprintcapture.utils;

public interface DebouncedClickHandler {
    long CLICK_DEBOUNCE_INTERVAL = 600; // ms
    long[] lastClickTimeHolder = new long[]{0}; // workaround for interface field mutability

    default boolean isSingleClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTimeHolder[0] < CLICK_DEBOUNCE_INTERVAL) {
            return false;
        }
        lastClickTimeHolder[0] = currentTime;
        return true;
    }
}

