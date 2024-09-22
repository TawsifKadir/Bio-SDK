package com.kit.fingerprintcapture.manager;

import com.morpho.morphosmart.sdk.TemplateList;

import java.util.Map;

public interface IDeviceManager {
    long initDevice();
    long openDevice();
    long startCapture();
    long closeDevice();
    long deInitDevice();
    boolean isDeviceOpen();
    boolean isPermissionAcquired();

}
