package com.kit.photocapture.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;


import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
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

            Mat result1 = new Mat();
            faceRecognizer.alignCrop(image1, faceBox1,result1);
            faceRecognizer.extractFeature(result1,result1);

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

            Mat result2 = new Mat();
            faceRecognizer.alignCrop(image2, faceBox2,result2);
            faceRecognizer.extractFeature(result2,result2);

            // 6. Compare features
            double similarity = faceRecognizer.compareFeatures(result1, result2);
            boolean isMatch = faceRecognizer.isMatch(result1, result2, MATCH_THRESHOLD);

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

    public static void compareTwoFaceMats(Context context, Mat image1, Mat image2, String person) {
        try {


            Log.d("ImageCheck", "image1 size: " + image1.size());
            Log.d("ImageCheck", "image2 size: " + image2.size());
            Log.d("ImageCheck", "image1 pixel[0]: " + Arrays.toString(image1.get(0, 0)));
            Log.d("ImageCheck", "image2 pixel[0]: " + Arrays.toString(image2.get(0, 0)));



            String hash1 = getImageHash(image1);
            String hash2 = getImageHash(image2);

            Log.d("ImageHash", "image1 hash: " + hash1);
            Log.d("ImageHash", "image2 hash: " + hash2);

            if (hash1.equals(hash2)) {
                Log.e(TAG, "Both input images are identical. Aborting comparison.");
                return;
            }

            YunetFaceDetectionImpl faceDetector1 = new YunetFaceDetectionImpl(context);
            faceDetector1.loadDetector();



            FaceRecognizer faceRecognizer = new SFaceRecognitionModelImpl(context);
            faceRecognizer.loadRecognizer();

            // Detect first face
            Mat faces1 = new Mat();
            faceDetector1.setInputSize(new Size(image1.cols(), image1.rows()));
            faceDetector1.detect(image1, faces1);

            if (faces1.rows() == 0) {
                Log.e(TAG, "No face found in image 1");
                return;
            }else {
                Log.e(TAG, "face found in image 1: "+ faces1.rows());
            }

            YunetFaceDetectionImpl faceDetector2 = new YunetFaceDetectionImpl(context);
            faceDetector2.loadDetector();

            // Detect second face
            Mat faces2 = new Mat();
            faceDetector2.setInputSize(new Size(image2.cols(), image2.rows()));
            faceDetector2.detect(image2, faces2);

            if (faces2.rows() == 0) {
                Log.e(TAG, "No face found in image 2");
                return;
            }
            else {
                Log.e(TAG, "face found in image 2: "+ faces2.rows());
            }

            float[] faceData1 = new float[14];
            faces1.get(0, 0, faceData1);
            Mat faceBox1 = new Mat(1, 4, CvType.CV_32FC1);
            faceBox1.put(0, 0, faceData1[0], faceData1[1], faceData1[2], faceData1[3]);

            float[] faceData2 = new float[14];
            faces2.get(0, 0, faceData2);
            Mat faceBox2 = new Mat(1, 4, CvType.CV_32FC1);
            faceBox2.put(0, 0, faceData2[0], faceData2[1], faceData2[2], faceData2[3]);

            Mat result1 = new Mat(), result2 = new Mat();
            faceRecognizer.alignCrop(image1, faceBox1, result1);
            faceRecognizer.alignCrop(image2, faceBox2, result2);

            Mat feature1 = new Mat(), feature2 = new Mat();
            faceRecognizer.extractFeature(result1, feature1);
            faceRecognizer.extractFeature(result2, feature2);

//
//            double pixel1 = result1.get(0, 0)[0];
//            double pixel2 = result2.get(0, 0)[0];
//            Log.d(TAG, "Pixel (0,0) - Result1: " + pixel1 + ", Result2: " + pixel2);
//
//            double[] f1 = feature1.get(0, 0);
//            double[] f2 = feature2.get(0, 0);
//
//            Log.d(TAG, "Feature1: " + Arrays.toString(f1));
//            Log.d(TAG, "Feature2: " + Arrays.toString(f2));


            double similarity = faceRecognizer.compareFeatures(feature1, feature2);
            boolean isMatch = faceRecognizer.isMatch(feature1, feature2, MATCH_THRESHOLD);

            String result = String.format("%s\nSimilarity: %.2f%% — Match: %s", person,
                    similarity * 100, isMatch ? "YES ✅" : "NO ❌");

            Log.i(TAG, result);
            Toast.makeText(context, result, Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e(TAG, "compareTwoFaceMats failed", e);
        }
    }


    public static String getImageHash(Mat img) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".jpg", img, buffer);
        byte[] byteArray = buffer.toArray();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(byteArray);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
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
