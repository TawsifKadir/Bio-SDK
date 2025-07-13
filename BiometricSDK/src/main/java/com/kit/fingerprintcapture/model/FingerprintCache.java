package com.kit.fingerprintcapture.model;

import java.util.List;

public class FingerprintCache {
    private static FingerprintCache instance;
    private List<FingerprintCacheEntry> fingerList;

    private FingerprintCache() {}

    public static FingerprintCache getInstance() {
        if (instance == null) {
            instance = new FingerprintCache();
        }
        return instance;
    }

    public void setFingerList(List<FingerprintCacheEntry> fingerList) {
        this.fingerList = fingerList;
    }

    public List<FingerprintCacheEntry> getFingerList() {
        return fingerList;
    }

    public void clear() {
        fingerList = null;
    }
}
