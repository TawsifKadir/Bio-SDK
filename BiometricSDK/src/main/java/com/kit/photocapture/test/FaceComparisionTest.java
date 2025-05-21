package com.kit.photocapture.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;


import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FaceComparisionTest {

    private static final String TAG = "FaceComparisonTest";
    private static final float MATCH_THRESHOLD = 0.6f; // Adjust based on your requirements

    public static void compareTwoFaces(Context context, int imageResId1, int imageResId2, String person) {
        // Initialize OpenCV if not already done
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
            return;
        }

        try {
            // 1. Load both images
            Mat image1 = loadImageFromResources(context, imageResId1);
            Mat image2 = loadImageFromResources(context, imageResId2);

            if (image1.empty() || image2.empty()) {
                Log.e(TAG, "Failed to load one or both images");
                return;
            }

            // 2. Initialize face detector
            YunetFaceDetectionImpl faceDetector = new YunetFaceDetectionImpl(context);
            faceDetector.loadDetector();
            // 3. Initialize face recognizer
            FaceRecognizer faceRecognizer = new SFaceRecognitionModelImpl(context);
            faceRecognizer.loadRecognizer();

            // 4. Process first image
            Mat faces1 = new Mat();

            Size size1 = faces1.size();
            double aspectRatio1 = size1.width/ size1.height;

            Mat resizedInput = new Mat();
            Mat resizedInput2 = new Mat();

            Imgproc.resize(image1, resizedInput, FaceDetectionModel.DEFAULT_INPUT_SIZE, aspectRatio1);  // mInputSize = new Size(320, 320)

            faceDetector.detect(resizedInput, faces1);
            Log.d(TAG, "Detected " + faces1.rows() + " faces in image 1");

            if (faces1.rows() == 0) {
                Log.e(TAG, "No faces detected in image 1");
                return;
            }

            // Get first face from image 1
            float[] faceData1 = new float[14];
            faces1.get(0, 0, faceData1);
            Mat faceBox1 = new Mat(1, 4, org.opencv.core.CvType.CV_32FC1);
            faceBox1.put(0, 0, faceData1[0], faceData1[1], faceData1[2], faceData1[3]);
            Mat alignedFace1 = faceRecognizer.alignFace(image1, faceBox1);
            Mat features1 = faceRecognizer.extractFeature(alignedFace1);

            // 5. Process second image
            Mat faces2 = new Mat();
            Size size2 = faces2.size();

            double aspectRatio2 = size2.width/ size2.height;

            Imgproc.resize(image2, resizedInput2, FaceDetectionModel.DEFAULT_INPUT_SIZE, aspectRatio2);  // mInputSize = new Size(320, 320)


            faceDetector.detect(resizedInput2, faces2);
            Log.d(TAG, "Detected " + faces2.rows() + " faces in image 2");

            resizedInput.release();
            resizedInput2.release();

            if (faces2.rows() == 0) {
                Log.e(TAG, "No faces detected in image 2");
                return;
            }

            // Get first face from image 2
            float[] faceData2 = new float[14];
            faces2.get(0, 0, faceData2);
            Mat faceBox2 = new Mat(1, 4, org.opencv.core.CvType.CV_32FC1);
            faceBox2.put(0, 0, faceData2[0], faceData2[1], faceData2[2], faceData2[3]);
            Mat alignedFace2 = faceRecognizer.alignFace(image2, faceBox2);
            Mat features2 = faceRecognizer.extractFeature(alignedFace2);

            // 6. Compare features
            double similarity = faceRecognizer.compareFeatures(features1, features2);
            boolean isMatch = faceRecognizer.isMatch(features1, features2, MATCH_THRESHOLD);

            Log.d(TAG, String.format("%s Face similarity: %.4f, Match: %b (Threshold: %.2f)", person,
                    similarity, isMatch, MATCH_THRESHOLD));

            // 7. Draw results on both images
            drawDetectionResult(image1, faceData1, "Image 1");
            drawDetectionResult(image2, faceData2, "Image 2");

            // 8. Create a combined result image
            Mat combinedResult = combineImages(image1, image2, similarity, isMatch, person);

            // 9. Save the result
            saveResultImage(context, combinedResult);

        } catch (Exception e) {
            Log.e(TAG, "Face comparison test failed", e);
        }
    }

    private static Mat loadImageFromResources(Context context, int resId) {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            if (mat.channels() == 4) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
            } else if (mat.channels() == 1) {
                Imgproc.cvtColor(mat, mat, Imgproc.COLOR_GRAY2RGB);
            }

            return mat;
        } catch (Exception e) {
            Log.e(TAG, "Error loading image", e);
            return new Mat();
        }
    }

    private static void drawDetectionResult(Mat image, float[] faceData, String label) {
        Rect rect = new Rect(
                (int) faceData[0],
                (int) faceData[1],
                (int) faceData[2],
                (int) faceData[3]);

        Scalar color = new Scalar(0, 255, 0); // Green rectangle
        Imgproc.rectangle(image, rect, color, 2);

        Imgproc.putText(image, label,
                new org.opencv.core.Point(rect.x, rect.y - 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 1);
    }

    private static Mat combineImages(Mat image1, Mat image2, double similarity, boolean isMatch, String message) {
        // Resize images to same height if needed
        int maxHeight = Math.max(image1.rows(), image2.rows());
        int maxWidth = Math.max(image1.cols(), image2.cols());

        Mat resized1 = new Mat();
        Mat resized2 = new Mat();

        Size size = new Size(maxWidth, maxHeight);
        Imgproc.resize(image1, resized1, size);
        Imgproc.resize(image2, resized2, size);

        List<Mat> imagesToCombine = new ArrayList<>();
        imagesToCombine.add(resized1);
        imagesToCombine.add(resized2);
        // Combine horizontally
        Mat combined = new Mat();
        Core.hconcat(imagesToCombine, combined);

        // Add comparison result text
        String resultText = String.format("%s Similarity: %.2f - %s", message,
                similarity, isMatch ? "MATCH" : "NO MATCH");

        Scalar textColor = isMatch ? new Scalar(0, 255, 0) : new Scalar(0, 0, 255);
        Imgproc.putText(combined, resultText,
                new org.opencv.core.Point(20, 30),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, textColor, 2);

        return combined;
    }

    private static void saveResultImage(Context context, Mat image) {
        File outputFile = new File(context.getExternalFilesDir(null), "face_comparison_result.jpg");
        Imgcodecs.imwrite(outputFile.getAbsolutePath(), image);
        Log.d(TAG, "Comparison result saved to: " + outputFile.getAbsolutePath());
    }
}
