package com.kit.fingerprintcapture.model;

import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class Fingerprint {
    private FingerprintID fingerprintID;
    private FingerprintUI fingerprintUI;
    private FingerprintData fingerprintData;

    private FingerprintStatus status;


    public Fingerprint(FingerprintID fingerprintID, FingerprintUI fingerprintUI, FingerprintData fingerprintData, FingerprintStatus state) {
        this.fingerprintID = fingerprintID;
        this.fingerprintUI = fingerprintUI;
        this.fingerprintData = fingerprintData;
        this.status = state;
    }

    public FingerprintID getFingerprintID() {
        return fingerprintID;
    }

    public void setFingerprintID(FingerprintID fingerprintID) {
        this.fingerprintID = fingerprintID;
    }

    public FingerprintUI getFingerprintUI() {
        return fingerprintUI;
    }

    public void setFingerprintUI(FingerprintUI fingerprintUI) {
        this.fingerprintUI = fingerprintUI;
    }

    public FingerprintData getFingerprintData() {
        return fingerprintData;
    }

    public void setFingerprintData(FingerprintData fingerprintData) {
        this.fingerprintData = fingerprintData;
    }

    public FingerprintStatus getStatus() {
        return status;
    }

    public void setStatus(FingerprintStatus state) {
        this.status = state;
    }

    public void reset(){


    }

    public static Fingerprint newInstance(View v, FingerprintID fingerprintID, int fpBtnID, int fpMarkerID, int fpScoreTxtID){
        ImageButton fpBtn = (ImageButton)v.findViewById(fpBtnID);
        ImageView fpMarker = (ImageView)v.findViewById(fpMarkerID);
        TextView fpScoreTxt = (TextView)v.findViewById(fpScoreTxtID);

        FingerprintData fpData = new FingerprintData();
        fpData.setFingerprintId(fingerprintID);

        FingerprintUI fpUI = new FingerprintUI();
        fpUI.setFingerprintBtn(fpBtn);
        fpUI.setFingerprintMarker(fpMarker);
        fpUI.setFingerprintScore(fpScoreTxt);

        Fingerprint fPrint = new Fingerprint(fingerprintID,fpUI,fpData,FingerprintStatus.NOT_CAPTURED);

        return fPrint;
    }



}
