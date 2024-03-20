package com.kit.fingerprintcapture.model;

public enum FingerprintStatus {
    NOT_CAPTURED(1),
    CAPTURE_IN_PROGRESS(2),
    CAPTURED(3);

    private int id;

    private FingerprintStatus(int id){
        this.id=id;
    }

    public int getId(){
        return this.id;
    }
}
