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
        copyFingerprintData(fingerprintData);
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
        if(this.fingerprintData!=null) {
            dest.writeInt(this.fingerprintData.length);
            dest.writeByteArray(this.fingerprintData);
        }
        else {
            dest.writeInt(0);
        }

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
        copyFingerprintData(fingerprintData);
    }




    public long getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(long qualityScore) {
        this.qualityScore = qualityScore;
    }

    private void copyFingerprintData(byte[] fingerprintData){
        if(fingerprintData!=null) {
            this.fingerprintData = new byte[fingerprintData.length];
            System.arraycopy(fingerprintData,0,this.fingerprintData,0,fingerprintData.length);
        }
    }


}
