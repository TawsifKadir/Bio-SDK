package com.kit.photocapture.model.recognizer;

import android.content.Context;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.objdetect.FaceRecognizerSF;

import com.kit.photocapture.model.loader.RawModelLoader;
import com.kit.photocapture.model.type.ModelType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FaceRecognitionModel {

    // Default configuration constants
    public static final int DEFAULT_BACKEND_ID = 0;  // Default OpenCV backend
    public static final int DEFAULT_TARGET_ID = 0;     // CPU target
    public static final int DEFAULT_DISTANCE_TYPE = FaceRecognizerSF.FR_COSINE;
    public static final float DEFAULT_MATCH_THRESHOLD = 0.5f;

    private final FaceRecognizerSF faceRecognizer;
    private File modelFile;

    public FaceRecognitionModel(Context context) {
        this(context, DEFAULT_BACKEND_ID, DEFAULT_TARGET_ID);
    }

    public FaceRecognitionModel(Context context, int backendId, int targetId) {
        // Create model file from raw resource
        modelFile = createModelFileFromRaw(context, ModelType.FACE_RECOGNITION);

        if (modelFile == null || !modelFile.exists()) {
            throw new RuntimeException("Could not create FaceRecognition model file.");
        }

        // Create face recognizer
        faceRecognizer = FaceRecognizerSF.create(
                modelFile.getAbsolutePath(),
                "",  // Empty config for ONNX model
                backendId,
                targetId
        );

        if (faceRecognizer == null) {
            throw new RuntimeException("Failed to create FaceRecognizerSF.");
        }

        Log.i("FaceRecognitionModel",
                String.format("FaceRecognizerSF initialized with backend: %d, target: %d",
                        backendId, targetId));
    }

    private File createModelFileFromRaw(Context context, ModelType modelType) {
        File modelFile = new File(context.getCacheDir(), modelType.getFileName());

        // If file already exists and is valid, return it
        if (modelFile.exists() && modelFile.length() > 0) {
            return modelFile;
        }

        // Copy from raw resources to cache file
        try (InputStream inputStream = context.getResources().openRawResource(modelType.getResId());
             FileOutputStream outputStream = new FileOutputStream(modelFile)) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            return modelFile;
        } catch (IOException e) {
            Log.e("FaceRecognitionModel", "Error creating model file", e);
            return null;
        }
    }

    public FaceRecognizerSF getRecognizer() {
        return faceRecognizer;
    }

}