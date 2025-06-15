package com.kit.fingerprintcapture.callback;

import com.kit.fingerprintcapture.model.FingerprintCaptureItem;

public interface FingerprintCaptureCallback {
    void onCaptureStart(FingerprintCaptureItem fp);
    void onCaptureEnd(FingerprintCaptureItem fp);
    void onCaptureStop(FingerprintCaptureItem fp);
    void onCaptureFailed(FingerprintCaptureItem fp);

}
