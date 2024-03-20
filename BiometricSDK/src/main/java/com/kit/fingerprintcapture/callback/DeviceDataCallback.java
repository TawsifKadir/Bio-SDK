package com.kit.fingerprintcapture.callback;

import android.graphics.Bitmap;

public interface DeviceDataCallback {
    public void onFingerprintData(byte[] imgData,int width,int height,int qualityScore,long captureResult);
    public void onFingerprintPreview(Bitmap img, int width, int height);
    public void onCaptureCmd(String cmd);
    public void onCaptureError(String Error);
}
