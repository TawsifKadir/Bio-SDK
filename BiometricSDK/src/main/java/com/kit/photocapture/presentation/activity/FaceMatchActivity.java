package com.kit.photocapture.presentation.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.kit.biometricsdk.R;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;

public class FaceMatchActivity extends Activity {


    private static final int REQUEST_CODE_IMAGE1 = 101;
    private static final int REQUEST_CODE_IMAGE2 = 102;


    private static final Logger log = LoggerFactory.getLogger(FaceMatchActivity.class);
    private ImageView image1View, image2View;
    private Button matchButton;

    private Bitmap bitmap1, bitmap2;
    private static final float MATCH_THRESHOLD = 0.6f;
    private static final String TAG = "FaceMatchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_match);

        image1View = findViewById(R.id.image1);
        image2View = findViewById(R.id.image2);
        matchButton = findViewById(R.id.btn_match);

//        bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.salman);
//        // Load the most recent photo from SavedPhotos directory
//        File savedPhotosDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SavedPhotos");
//        if (savedPhotosDir.exists() && savedPhotosDir.isDirectory()) {
//            File[] files = savedPhotosDir.listFiles((dir, name) -> name.endsWith(".jpg"));
//            if (files != null && files.length > 0) {
//                // Sort files by last modified to get the latest one
//                Arrays.sort(files, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
//                File latestFile = files[0]; // most recently saved photo
//
//                bitmap1 = BitmapFactory.decodeFile(latestFile.getAbsolutePath());
//            } else {
//                Toast.makeText(this, "No saved photo found.", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(this, "SavedPhotos directory not found.", Toast.LENGTH_SHORT).show();
//        }


        File specificFile = new File(
                getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/SavedPhotos",
                "full_photo_1748864348734.jpg"
        );

        if (specificFile.exists()) {
            bitmap1 = BitmapFactory.decodeFile(specificFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "Specific photo not found.", Toast.LENGTH_SHORT).show();
        }


        bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.rafiul2);

        image1View.setImageBitmap(bitmap1);
        image2View.setImageBitmap(bitmap2);

        matchButton.setOnClickListener(v -> compareFaces());

        image1View.setOnClickListener(v -> {
            Intent intent = new Intent(FaceMatchActivity.this, PhotoCaptureActivity2.class);
            startActivityForResult(intent, REQUEST_CODE_IMAGE1);
            Log.d(TAG, "PhotoCaptureActivity2 started for image1");
        });

        image2View.setOnClickListener(v -> {
            Intent intent = new Intent(FaceMatchActivity.this, PhotoCaptureActivity2.class);
            startActivityForResult(intent, REQUEST_CODE_IMAGE2);
            Log.d(TAG, "PhotoCaptureActivity2 started for image2");
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult called: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (resultCode == RESULT_OK && data != null) {
            String returnedPath = data.getStringExtra(CapturedPhotoPreviewActivity.EXTRA_PHOTO_PATH);
            Log.d(TAG, "Received image path: " + returnedPath);

            if (returnedPath != null) {
                Bitmap resultBitmap = BitmapFactory.decodeFile(returnedPath);

                if (requestCode == REQUEST_CODE_IMAGE1) {
                    bitmap1 = resultBitmap;
                    image1View.setImageBitmap(bitmap1);
                    Log.d(TAG, "Image1 updated");
                } else if (requestCode == REQUEST_CODE_IMAGE2) {
                    bitmap2 = resultBitmap;
                    image2View.setImageBitmap(bitmap2);
                    Log.d(TAG, "Image2 updated");
                }
            } else {
                Log.e(TAG, "Returned path is null");
            }
        } else {
            Log.e(TAG, "Data is null or result not OK");
        }
    }



    private void compareFaces() {
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV failed to load", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Convert Bitmaps to Mat
            Mat mat1 = new Mat();
            Mat mat2 = new Mat();
            Utils.bitmapToMat(bitmap1, mat1);
            Utils.bitmapToMat(bitmap2, mat2);

            mat1 = convertToRGB(mat1);
            mat2 = convertToRGB(mat2);

            // Resize both to 320x320
            Size modelInputSize = FaceDetectionModel.DEFAULT_INPUT_SIZE;
            Imgproc.resize(mat1, mat1, modelInputSize);
            Imgproc.resize(mat2, mat2, modelInputSize);

            // Face Detector setup
            YunetFaceDetectionImpl detector = new YunetFaceDetectionImpl(this);
            detector.loadDetector();
            detector.setInputSize(modelInputSize);

            Mat faces1 = new Mat();
            Mat faces2 = new Mat();

            detector.detect(mat1, faces1);

            detector.detect(mat2, faces2);

            if (faces1.rows() == 0 && faces2.rows() == 0) {
                Toast.makeText(this, "No face detected in  both images", Toast.LENGTH_LONG).show();
                return;
            } else if (faces1.rows() == 0 ) {
                Toast.makeText(this, "No face detected in faces1 images", Toast.LENGTH_LONG).show();
                return;
            }
            else if (faces2.rows() == 0) {
                Toast.makeText(this, "No face detected in faces2 images", Toast.LENGTH_LONG).show();
                return;
            }

            // Recognizer pipeline
            FaceRecognizer recognizer = new SFaceRecognitionModelImpl(this);
            recognizer.loadRecognizer();

            Mat aligned1 = new Mat();
            Mat aligned2 = new Mat();
            recognizer.alignCrop(mat1, faces1.row(0), aligned1);
            recognizer.alignCrop(mat2, faces2.row(0), aligned2);


            Log.d("Debug", "Aligned1 size: " + aligned1.size());
            Log.d("Debug", "Aligned2 size: " + aligned2.size());
            Log.d("Debug", "Aligned1 pixel: " + aligned1.get(0, 0)[0]);
            Log.d("Debug", "Aligned2 pixel: " + aligned2.get(0, 0)[0]);
//            Log.d("FaceBox", "Face1 Box: " + faceBox1.dump());
//            Log.d("FaceBox", "Face2 Box: " + faceBox2.dump());




            Mat feature1 = new Mat();
            Mat feature2 = new Mat();
            recognizer.extractFeature(aligned1, feature1);
            feature1 = feature1.clone();

            recognizer.extractFeature(aligned2, feature2);
            feature2 = feature2.clone();



            logFeatureVector(feature1, "Feature1");
            logFeatureVector(feature2, "Feature2");


            Log.d(TAG, "compareFaces() called: " +  feature1.size());

            double similarity = recognizer.compareFeatures(feature1, feature2);
            boolean isMatch = recognizer.isMatch(feature1, feature2, MATCH_THRESHOLD);

            String result = String.format("Similarity: %.2f%%\nMatch: %s",
                    similarity * 100, isMatch ? "YES ✅" : "NO ❌");

            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            Log.i(TAG, result);

        } catch (Exception e) {
            Log.e(TAG, "Face match error", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Mat convertToRGB(Mat input) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_RGBA2RGB);
        return rgb;
    }
    private void logFeatureVector(Mat feature, String tag) {
        if (feature.rows() == 1 && feature.cols() > 0 && feature.type() == CvType.CV_32FC1) {
            StringBuilder sb = new StringBuilder();
            sb.append(tag).append(" [");
            for (int i = 0; i < feature.cols(); i++) {
                float[] val = new float[1];
                feature.get(0, i, val);
                sb.append(String.format("%.6f", val[0]));
                if (i < feature.cols() - 1) sb.append(", ");
            }
            sb.append("]");
            Log.d("FeatureVector", sb.toString());
        } else {
            Log.w("FeatureVector", tag + " is not a valid 1xN float matrix");
        }
    }


}
