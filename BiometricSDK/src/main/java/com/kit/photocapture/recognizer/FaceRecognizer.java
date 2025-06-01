package com.kit.photocapture.recognizer;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;

public interface FaceRecognizer {
    void loadRecognizer() throws Exception;
    void alignCrop(Mat srcImage, Mat faceBox, Mat result);

    void extractFeature(Mat alignedFace, Mat result);
    double compareFeatures(Mat feature1, Mat feature2);
    boolean isMatch(Mat feature1, Mat feature2, float threshold);
    boolean isMatch(Mat feature1, Mat feature2);
    /**
     * Releases model resources
     */
    void release();
}
