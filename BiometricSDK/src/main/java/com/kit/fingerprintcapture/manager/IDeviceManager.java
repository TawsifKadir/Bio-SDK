package com.kit.fingerprintcapture.manager;

import java.util.Map;

public interface IDeviceManager {

    /// Check for duplicate fingerprint from matcher
    /// Check for quality here
    void verifyFingerprint(byte[] imgData, int width, int height);
//    boolean matchFingerprint(List of Reference Template, current Template);


    /// Related to Device
    long initDevice();
    long openDevice();
    long startCapture();
    long closeDevice();
    long deInitDevice();
    boolean isDeviceOpen();
    boolean isPermissionAcquired();

}
