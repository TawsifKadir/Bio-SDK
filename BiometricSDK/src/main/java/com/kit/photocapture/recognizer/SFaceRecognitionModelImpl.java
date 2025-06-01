package com.kit.photocapture.recognizer;

import android.content.Context;
import android.util.Log;

import com.kit.photocapture.model.type.ModelType;

import org.opencv.core.Mat;
import org.opencv.objdetect.FaceRecognizerSF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class SFaceRecognitionModelImpl implements FaceRecognizer {

    private static final int DEFAULT_BACKEND_ID = 0;  // Default OpenCV backend
    private static final int DEFAULT_TARGET_ID = 0;   // CPU target
    private static final int DEFAULT_DISTANCE_TYPE = FaceRecognizerSF.FR_COSINE;
    private static final float DEFAULT_MATCH_THRESHOLD = 0.5f;

    private final Context context;
    private FaceRecognizerSF faceRecognizer;
    private File modelFile;

    public SFaceRecognitionModelImpl(Context context) {
        this.context = context;
        this.faceRecognizer = null;
    }

    @Override
    public void loadRecognizer() throws Exception {
        // Create model file from raw resource
        modelFile = createModelFileFromRaw(context, ModelType.FACE_RECOGNITION);

        if (modelFile == null || !modelFile.exists()) {
            throw new Exception("Could not create FaceRecognition model file.");
        }

        // Create face recognizer
        faceRecognizer = FaceRecognizerSF.create(
                modelFile.getAbsolutePath(),
                "",  // Empty config for ONNX model
                DEFAULT_BACKEND_ID,
                DEFAULT_TARGET_ID
        );

        if (faceRecognizer == null) {
            throw new Exception("Failed to create FaceRecognizerSF.");
        }

        Log.i("SFaceRecognitionModel", "FaceRecognizerSF initialized");
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
            Log.e("SFaceRecognitionModel", "Error creating model file", e);
            return null;
        }
    }

    @Override
    public void alignCrop(Mat srcImage, Mat faceBox, Mat result) {
        if (faceRecognizer == null) {
            throw new IllegalStateException("Recognizer not loaded. Call loadRecognizer() first.");
        }

        faceRecognizer.alignCrop(srcImage, faceBox, result);
    }

    @Override
    public void extractFeature(Mat alignedFace, Mat result) {
        if (faceRecognizer == null) {
            throw new IllegalStateException("Recognizer not loaded. Call loadRecognizer() first.");
        }
        faceRecognizer.feature(alignedFace, result);
    }

    @Override
    public double compareFeatures(Mat feature1, Mat feature2) {
        if (faceRecognizer == null) {
            throw new IllegalStateException("Recognizer not loaded. Call loadRecognizer() first.");
        }

        return faceRecognizer.match(feature1, feature2, DEFAULT_DISTANCE_TYPE);
    }

    @Override
    public boolean isMatch(Mat feature1, Mat feature2, float threshold) {
        double similarity = compareFeatures(feature1, feature2);
        return similarity >= threshold;
    }

    @Override
    public boolean isMatch(Mat feature1, Mat feature2) {
        return isMatch(feature1, feature2, DEFAULT_MATCH_THRESHOLD);
    }

    @Override
    public void release() {
        if (faceRecognizer != null) {
            // FaceRecognizerSF doesn't have a direct release method in Java,
            // but it will be handled by finalize()
        }
        if (modelFile != null && modelFile.exists()) {
            modelFile.delete();
        }
    }
}
