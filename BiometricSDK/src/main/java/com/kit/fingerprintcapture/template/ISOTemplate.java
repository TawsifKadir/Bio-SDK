package com.kit.fingerprintcapture.template;

import com.fasterxml.jackson.annotation.JsonAnyGetter;


public class ISOTemplate {
    private byte[] mIsoTemplate;
    private int mIsoTemplateSize;

    public ISOTemplate(byte[] mIsoTemplate, int mIsoTemplateSize) {
        this.mIsoTemplate = mIsoTemplate;
        this.mIsoTemplateSize = mIsoTemplateSize;
    }

    public byte[] getIsoTemplate() {
        return mIsoTemplate;
    }

    public void setIsoTemplate(byte[] mIsoTemplate) {
        this.mIsoTemplate = mIsoTemplate;
    }

    public int getIsoTemplateSize() {
        return mIsoTemplateSize;
    }

    public void setIsoTemplateSize(int mIsoTemplateSize) {
        this.mIsoTemplateSize = mIsoTemplateSize;
    }
}
