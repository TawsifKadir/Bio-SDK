package com.kit.fingerprintcapture.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FingerprintData implements Parcelable {
    private FingerprintID id;
    private byte[] fingerprintData;
    private long qualityScore;
    private byte[] isoTemplate;

    public FingerprintData() {
        this.fingerprintData = null;
        this.qualityScore = 0;
    }

    public FingerprintData(FingerprintID id, byte[] fingerprintData, long qualityScore, byte[] isoTemplate) {
        this.id = id;
        this.fingerprintData = fingerprintData;
        this.qualityScore = qualityScore;
        this.isoTemplate = isoTemplate;
    }

    public FingerprintID getFingerprintId() {
        return id;
    }

    public void setFingerprintId(FingerprintID id) {
        this.id = id;
    }

    protected FingerprintData(Parcel in) {
        this.id = FingerprintID.getFingerprintID(in.readInt());

        // Read fingerprintData
        int dataLen = in.readInt();
        fingerprintData = new byte[dataLen];
        in.readByteArray(fingerprintData);

        qualityScore = in.readLong();

        // Read isoTemplate
        int templateLen = in.readInt();
        if (templateLen > 0) {
            isoTemplate = new byte[templateLen];
            in.readByteArray(isoTemplate);
        } else {
            isoTemplate = new byte[0];
        }
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id.getID());

        // Write fingerprintData
        if (this.fingerprintData != null)
            dest.writeInt(this.fingerprintData.length);
        else
            dest.writeInt(0);
        dest.writeByteArray(this.fingerprintData);

        dest.writeLong(qualityScore);

        // Write isoTemplate
        if (this.isoTemplate != null)
            dest.writeInt(this.isoTemplate.length);
        else
            dest.writeInt(0);
        dest.writeByteArray(this.isoTemplate);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<FingerprintData> CREATOR = new Creator<FingerprintData>() {
        @Override
        public FingerprintData createFromParcel(Parcel in) {
            return new FingerprintData(in);
        }

        @Override
        public FingerprintData[] newArray(int size) {
            return new FingerprintData[size];
        }
    };

    public byte[] getFingerprintData() {
        return fingerprintData;
    }

    public void setFingerprintData(byte[] fingerprintData) {
        this.fingerprintData = fingerprintData;
    }

    public long getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(long qualityScore) {
        this.qualityScore = qualityScore;
    }

    public byte[] getIsoTemplate() {
        return isoTemplate;
    }

    public void setIsoTemplate(byte[] isoTemplate) {
        this.isoTemplate = isoTemplate;
    }


    @Override
    public String toString() {
        return "FingerprintData{" +
                "id=" + (id != null ? id.getID() : "N/A") +
                ", qualityScore=" + qualityScore +
                ", fingerprintDataLength=" + (fingerprintData != null ? fingerprintData.length : 0) +
                ", isoTemplateLength=" + (isoTemplate != null ? isoTemplate.length : 0) +
                '}';
    }
}
