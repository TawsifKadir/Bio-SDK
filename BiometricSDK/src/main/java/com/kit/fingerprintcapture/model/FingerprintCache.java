package com.kit.fingerprintcapture.model;

import java.util.List;

public class FingerprintCache {
    private static FingerprintCache instance;
    private List<FingerprintData> fingerList;

    private FingerprintCache() {}

    public static FingerprintCache getInstance() {
        if (instance == null) {
            instance = new FingerprintCache();
        }
        return instance;
    }

    public void setFingerList(List<FingerprintData> fingerList) {
        this.fingerList = fingerList;
    }

    public List<FingerprintData> getFingerList() {
        return fingerList;
    }

    public void clear() {
        fingerList = null;
    }
}
