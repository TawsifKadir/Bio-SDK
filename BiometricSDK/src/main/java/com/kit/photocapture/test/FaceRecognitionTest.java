package com.kit.photocapture.test;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class FaceRecognitionTest {

    private static final String TAG = "FaceRecognitionTest";
    private static final float MATCH_THRESHOLD = 0.6f; // Adjust based on your requirements

    public static void testFaceRecognition(Context context, int imageResId) {
        // Initialize OpenCV if not already done
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Log.e(TAG, "OpenCV initialization failed");
            return;
        }

        try {
            // 1. Load the test image
            Mat image = loadImageFromResources(context, imageResId);
            if (image.empty()) {
                Log.e(TAG, "Failed to load image");
                return;
            }

            // 2. Initialize face detector
            YunetFaceDetectionImpl faceDetector = new YunetFaceDetectionImpl(context, image.width(), image.height());
            faceDetector.loadDetector();

            // 3. Detect faces
            Mat faces = new Mat();
            faceDetector.detect(image, faces);
            Log.d(TAG, "Detected " + faces.rows() + " faces");

            if (faces.rows() == 0) {
                Log.e(TAG, "No faces detected");
                return;
            }
            // 4. Initialize face recognizer
            FaceRecognizer faceRecognizer = new SFaceRecognitionModelImpl(context);
            faceRecognizer.loadRecognizer();

            // 5. Process each face
            for (int i = 0; i < faces.rows(); i++) {
                // Get face bounding box (YU-Net format: [x, y, w, h, x_re, y_re, x_le, y_le, x_nt, y_nt, ...])
                float[] faceData = new float[14];
                faces.get(i, 0, faceData);

                // Create face box Mat for recognition (needs [x, y, w, h, confidence] format)
                Mat faceBox = new Mat(1, 4, org.opencv.core.CvType.CV_32FC1);
                faceBox.put(0, 0, faceData[0], faceData[1], faceData[2], faceData[3]);

                // 6. Align and extract features
                Mat result = new Mat();
                 faceRecognizer.alignCrop(image, faceBox,result);
                faceRecognizer.extractFeature(result,result);

                // 7. Compare with itself (should be a perfect match)
                double similarity = faceRecognizer.compareFeatures(result, result);
                boolean isMatch = faceRecognizer.isMatch(result, result, MATCH_THRESHOLD);

                Log.d(TAG, String.format("Face %d - Similarity with self: %.4f, Match: %b",
                        i, similarity, isMatch));

                // 8. Draw results on image (optional)
                drawDetectionResult(image, faceData, isMatch);
            }

            // 9. Save or display the result
            saveResultImage(context, image);

        } catch (Exception e) {
            Log.e(TAG, "Face recognition test failed", e);
        }
    }

    private static Mat loadImageFromResources(Context context, int resId) {
        try {
            // Load bitmap from resources
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resId);

            // Convert to OpenCV Mat
            Mat mat = new Mat();
            Utils.bitmapToMat(bitmap, mat);

            // Convert to RGB if needed
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

    private static void drawDetectionResult(Mat image, float[] faceData, boolean isMatch) {
        // Draw face rectangle
        Rect rect = new Rect(
                (int) faceData[0],
                (int) faceData[1],
                (int) faceData[2],
                (int) faceData[3]);

        Scalar color = isMatch ? new Scalar(0, 255, 0) : new Scalar(0, 0, 255);
        Imgproc.rectangle(image, rect, color, 2);

        // Draw match status text
        String status = isMatch ? "MATCH" : "NO MATCH";
        Imgproc.putText(image, status,
                new org.opencv.core.Point(rect.x, rect.y - 10),
                Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 1);
    }

    private static void saveResultImage(Context context, Mat image) {
        File outputFile = new File(context.getExternalFilesDir(null), "face_recognition_result.jpg");
        Imgcodecs.imwrite(outputFile.getAbsolutePath(), image);
        Log.d(TAG, "Result saved to: " + outputFile.getAbsolutePath());
    }
}