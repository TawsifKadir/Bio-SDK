package com.kit.photocapture.util.faceDetectionUtils;

public class ComplianceResult {
    public final boolean isCompliant;
    public final String message;

    public ComplianceResult(boolean isCompliant, String message) {
        this.isCompliant = isCompliant;
        this.message = message;
    }
}
