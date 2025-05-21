package com.kit.photocapture.model.type;


import com.kit.biometricsdk.R;

public enum ModelType {
    FACE_DETECTION(R.raw.face_detection_yunet_2023mar, "face_detection_yunet_2023mar.onnx"),
    FACE_RECOGNITION(R.raw.face_recognition_sface_2021dec, "face_recognition_sface_2021dec.onnx");

    private final int resId;
    private final String fileName;

    ModelType(int resId, String fileName) {
        this.resId = resId;
        this.fileName = fileName;
    }

    public int getResId() {
        return resId;
    }

    public String getFileName() {
        return fileName;
    }
}
