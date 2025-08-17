package com.kit.fingerprintcapture.model;

import com.localafis.sourceafis.FingerprintTemplate;

public class FingerprintCacheEntry {
    private FingerprintID fingerprintId;
    private FingerprintTemplate rawTemplate;

    public FingerprintCacheEntry(FingerprintID fingerprintId, FingerprintTemplate rawTemplate) {
        this.fingerprintId = fingerprintId;
        this.rawTemplate = rawTemplate;
    }

    public FingerprintID getFingerprintId() {
        return fingerprintId;
    }

    public void setFingerprintId(FingerprintID fingerprintId) {
        this.fingerprintId = fingerprintId;
    }

    public FingerprintTemplate getRawTemplate() {
        return rawTemplate;
    }


    public void setRawImageData(FingerprintTemplate rawTemplate) {
        this.rawTemplate = rawTemplate;
    }
}
