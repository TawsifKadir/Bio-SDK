package com.kit.fingerprintcapture.callback;

import android.graphics.Bitmap;

public interface DeviceDataCallback {
    void onFingerprintData(byte[] imgData, int width, int height, int qualityScore, long captureResult);
    void onFingerprintPreview(Bitmap img, int width, int height);
    void onCaptureCmd(String cmd);
    void onCaptureError(String Error);
}
