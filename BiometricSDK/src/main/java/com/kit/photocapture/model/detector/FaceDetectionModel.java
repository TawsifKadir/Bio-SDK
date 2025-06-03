package com.kit.photocapture.model.detector;

import android.content.Context;
import android.util.Log;

import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;

import com.kit.photocapture.model.loader.RawModelLoader;
import com.kit.photocapture.model.type.ModelType;

public class FaceDetectionModel {

    // Default configuration constants
    public static final float DEFAULT_SCORE_THRESHOLD = 0.80f;
    public static final float DEFAULT_NMS_THRESHOLD = 0.4f;
    public static final int DEFAULT_TOP_K = 150;
    public static final Size DEFAULT_INPUT_SIZE = new Size(320, 320);

    private final FaceDetectorYN faceDetector;

    public FaceDetectionModel(Context context) {
        this(context, new MatOfByte(), DEFAULT_INPUT_SIZE,
                DEFAULT_SCORE_THRESHOLD, DEFAULT_NMS_THRESHOLD, DEFAULT_TOP_K);
    }

    public FaceDetectionModel(Context context, int width, int height) {
        this(context, new MatOfByte(), new Size(width, height),
                DEFAULT_SCORE_THRESHOLD, DEFAULT_NMS_THRESHOLD, DEFAULT_TOP_K);
    }

    public FaceDetectionModel(Context context, int width, int height, double aspectRatio) {
        this(context, new MatOfByte(), new Size(width, height/aspectRatio),
                DEFAULT_SCORE_THRESHOLD, DEFAULT_NMS_THRESHOLD, DEFAULT_TOP_K);
    }

    public FaceDetectionModel(Context context, MatOfByte config, int width, int height) {
        this(context, config, new Size(width, height),
                DEFAULT_SCORE_THRESHOLD, DEFAULT_NMS_THRESHOLD, DEFAULT_TOP_K);
    }

    public FaceDetectionModel(Context context, MatOfByte config, int width, int height, double aspectRatio) {
        this(context, config, new Size(width, height/aspectRatio),
                DEFAULT_SCORE_THRESHOLD, DEFAULT_NMS_THRESHOLD, DEFAULT_TOP_K);
    }

    public FaceDetectionModel(Context context, MatOfByte config, Size inputSize,
                              float scoreThreshold, float nmsThreshold, int topK) {
        byte[] modelBuffer = new RawModelLoader(context, ModelType.FACE_DETECTION).loadAsByteArray();

        if (modelBuffer == null) {
            throw new RuntimeException("Could not load FaceDetection model buffer.");
        }

        MatOfByte model = new MatOfByte(modelBuffer);

        // Create face detector with all configurable parameters
        faceDetector = FaceDetectorYN.create(
                "onnx",
                model,
                config,
                inputSize,
                scoreThreshold,
                nmsThreshold,
                topK
        );

        if (faceDetector == null) {
            throw new RuntimeException("Failed to create FaceDetectorYN.");
        }

        Log.i("FaceDetectionModel", "FaceDetectorYN initialized with size: " + inputSize);
    }

    public FaceDetectorYN getDetector() {
        return faceDetector;
    }
}