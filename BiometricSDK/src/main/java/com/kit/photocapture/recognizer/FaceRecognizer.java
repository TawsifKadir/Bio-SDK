package com.kit.photocapture.recognizer;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;

public interface FaceRecognizer {
    /**
     * Loads the face recognition model
     * @throws Exception if model fails to load
     */
    void loadRecognizer() throws Exception;

    /**
     * Aligns and crops a face image
     * @param srcImage Input image containing face
     * @param faceBox Face detection coordinates
     * @return Aligned face image
     */
    Mat alignFace(Mat srcImage, Mat faceBox);

    /**
     * Extracts feature vector from face image
     * @param alignedFace Aligned face image
     * @return Feature vector (1x128 float Mat)
     */
    Mat extractFeature(Mat alignedFace);

    /**
     * Compares two face feature vectors
     * @param feature1 First feature vector
     * @param feature2 Second feature vector
     * @return Similarity score (higher means more similar)
     */
    double compareFeatures(Mat feature1, Mat feature2);

    /**
     * Checks if two faces match based on threshold
     * @param feature1 First feature vector
     * @param feature2 Second feature vector
     * @param threshold Similarity threshold
     * @return true if faces match
     */
    boolean isMatch(Mat feature1, Mat feature2, float threshold);

    /**
     * Releases model resources
     */
    void release();
}
