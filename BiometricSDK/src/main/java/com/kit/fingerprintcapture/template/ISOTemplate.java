package com.kit.fingerprintcapture.template;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class ISOTemplate implements Serializable, Parcelable {
    private static final long serialVersionUID = 1L;

    private byte[] mIsoTemplate;
    private int mIsoTemplateSize;

    public ISOTemplate(byte[] mIsoTemplate, int mIsoTemplateSize) {
        this.mIsoTemplate = mIsoTemplate;
        this.mIsoTemplateSize = mIsoTemplateSize;
    }

    protected ISOTemplate(Parcel in) {
        mIsoTemplateSize = in.readInt();
        int templateLength = in.readInt();
        if (templateLength > 0) {
            mIsoTemplate = new byte[templateLength];
            in.readByteArray(mIsoTemplate);
        } else {
            mIsoTemplate = null;
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mIsoTemplateSize);
        if (mIsoTemplate != null) {
            dest.writeInt(mIsoTemplate.length);
            dest.writeByteArray(mIsoTemplate);
        } else {
            dest.writeInt(0);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ISOTemplate> CREATOR = new Creator<ISOTemplate>() {
        @Override
        public ISOTemplate createFromParcel(Parcel in) {
            return new ISOTemplate(in);
        }

        @Override
        public ISOTemplate[] newArray(int size) {
            return new ISOTemplate[size];
        }
    };

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
