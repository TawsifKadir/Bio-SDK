package com.kit.fingerprintcapture.model;

import java.util.Arrays;
import java.util.List;

public enum FingerprintCaptureType {
    REGISTRATION(1),
    VERIFICATION(2);

    private int id;

    private FingerprintCaptureType(int id){
        this.id=id;
    }

    public int getId() {
        return id;
    }

    private void setId(int id) {
        this.id = id;
    }

    public FingerprintCaptureType getFingerprintCaptureTypeByID(int id){
        List<FingerprintCaptureType> fpctList = Arrays.asList(FingerprintCaptureType.values());
        for(FingerprintCaptureType nowType:fpctList){
            if(nowType.getId()==id) return nowType;
        }
        return null;
    }
}
