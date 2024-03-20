package com.kit.fingerprintcapture.model;

import java.util.Arrays;
import java.util.List;

import java.util.Arrays;
import java.util.List;

public enum NoFingerprintReason {
    NoFingerprintImpression(1,"No Fingerprint"),
    NoFinger(2,"Missing Finger"),
    NoLeftHand(3,"Missing Left Hand"),
    NoRightHand(4,"Missing Right Hand"),
    NoBothHand(5,"Missing Both Hand"),
    Other(6,"Other (Specify)");

    private int noFingerprintReasonID;
    private String noFingerprintReasonText;

    NoFingerprintReason(int noFingerprintReasonID, String noFingerprintReasonText) {
        this.noFingerprintReasonID = noFingerprintReasonID;
        this.noFingerprintReasonText = noFingerprintReasonText;
    }

    public int getNoFingerprintReasonID() {
        return noFingerprintReasonID;
    }

    public void setNoFingerprintReasonID(int noFingerprintReasonID) {
        this.noFingerprintReasonID = noFingerprintReasonID;
    }

    public String getNoFingerprintReasonText() {
        return noFingerprintReasonText;
    }

    public void setNoFingerprintReasonText(String noFingerprintReasonText) {
        this.noFingerprintReasonText = noFingerprintReasonText;
    }
    public static NoFingerprintReason getNoFingerPrintReasonByID(int id){
        for(NoFingerprintReason nfr : NoFingerprintReason.values()){
            if(nfr.getNoFingerprintReasonID()==id){
                return nfr;
            }
        }
        return null;
    }
    public static List<NoFingerprintReason> getReasonList(){

        NoFingerprintReason[] nfr = NoFingerprintReason.values();
        return Arrays.asList(nfr);
    }


}
