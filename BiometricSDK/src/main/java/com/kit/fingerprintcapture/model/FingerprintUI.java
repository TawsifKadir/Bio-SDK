package com.kit.fingerprintcapture.model;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class FingerprintUI {
private ImageButton fingerprintBtn;
private ImageView fingerprintMarker;
private TextView  fingerprintScore;

    public FingerprintUI() {

    }

    public TextView getFingerprintScore() {
        return fingerprintScore;
    }

    public void setFingerprintScore(TextView fingerprintScore) {
        this.fingerprintScore = fingerprintScore;
    }

    public ImageView getFingerprintMarker() {
        return fingerprintMarker;
    }

    public void setFingerprintMarker(ImageView fingerprintMarker) {
        this.fingerprintMarker = fingerprintMarker;
    }

    public ImageButton getFingerprintBtn() {
        return fingerprintBtn;
    }

    public void setFingerprintBtn(ImageButton fingerprintBtn) {
        this.fingerprintBtn = fingerprintBtn;
    }
}
