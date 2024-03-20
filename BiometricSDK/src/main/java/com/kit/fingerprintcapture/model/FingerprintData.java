package com.kit.fingerprintcapture.model;

import android.os.Parcel;
import android.os.Parcelable;

public class FingerprintData implements Parcelable {
    private FingerprintID id;
    private byte[] fingerprintData;
    private long qualityScore;

    public FingerprintData() {
        this.fingerprintData = null;
        this.qualityScore = 0;
    }

    public FingerprintData(FingerprintID id, byte[] fingerprintData, long qualityScore) {
        this.id = id;
        this.fingerprintData = fingerprintData;
        this.qualityScore = qualityScore;
    }

    public FingerprintID getFingerprintId() {
        return id;
    }

    public void setFingerprintId(FingerprintID id) {
        this.id = id;
    }

    protected FingerprintData(Parcel in) {
        this.id = FingerprintID.getFingerprintID(in.readInt());
        int dataLen = in.readInt();
        fingerprintData = new byte[dataLen];
        in.readByteArray(fingerprintData);
        qualityScore = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id.getID());
        if(this.fingerprintData!=null)
            dest.writeInt(this.fingerprintData.length);
        else
            dest.writeInt(0);
        dest.writeByteArray(this.fingerprintData);
        dest.writeLong(qualityScore);
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


}
