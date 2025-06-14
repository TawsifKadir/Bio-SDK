package com.kit.fingerprintcapture.callback;

import com.kit.fingerprintcapture.model.Fingerprint;

public interface FingerprintCaptureCallback {
    void onCaptureStart(Fingerprint fp);
    void onCaptureEnd(Fingerprint fp);
    void onCaptureStop(Fingerprint fp);
    void onCaptureFailed(Fingerprint fp);

}
