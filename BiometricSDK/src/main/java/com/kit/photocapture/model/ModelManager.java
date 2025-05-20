package com.kit.photocapture.model;

import android.content.Context;
import com.kit.photocapture.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognitionModel;

import org.opencv.core.MatOfByte;

public class ModelManager {

    private static FaceDetectionModel faceDetectionModel;
    private static FaceRecognitionModel faceRecognitionModel;

    public static FaceDetectionModel getFaceDetectionModel(Context context) {
        if (faceDetectionModel == null) {
            faceDetectionModel = new FaceDetectionModel(context);
        }
        return faceDetectionModel;
    }

    public static FaceDetectionModel getFaceDetectionModel(Context context, int width, int height) {
        if (faceDetectionModel == null) {
            faceDetectionModel = new FaceDetectionModel(context, width, height);
        }
        return faceDetectionModel;
    }

    public static FaceDetectionModel getFaceDetectionModel(Context context, MatOfByte config, int width, int height) {
        if (faceDetectionModel == null) {
            faceDetectionModel = new FaceDetectionModel(context, config, width, height);
        }
        return faceDetectionModel;
    }

//    public static FaceRecognitionModel getFaceDetectionModel(Context context) {
//        if (faceRecognitionModel == null) {
//            faceRecognitionModel = new FaceRecognitionModel(context);
//        }
//        return faceRecognitionModel;
//    }
}
