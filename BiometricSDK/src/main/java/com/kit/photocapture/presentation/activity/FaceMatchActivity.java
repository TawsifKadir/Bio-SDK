package com.kit.photocapture.presentation.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.kit.biometricsdk.R;
import com.kit.photocapture.util.faceRecongonization.FaceRecognizerHelper;
import com.kit.photocapture.util.faceRecongonization.LoadImageFromFile;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import javax.annotation.Nullable;


public class FaceMatchActivity extends Activity {


    private static final int REQUEST_CODE_IMAGE1 = 101;
    private static final int REQUEST_CODE_IMAGE2 = 102;



    private ImageView image1View, image2View;
    private Button matchButton;

    private TextView resultViewer;
    private Bitmap bitmap1, bitmap2;
    private ProgressBar progressBar;
    private static final float MATCH_THRESHOLD = 0.6f;
    private static final String TAG = "FaceMatchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_match);

        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV failed to load", Toast.LENGTH_SHORT).show();
            return;
        }

        initViews();
        loadBitmaps();

        image1View.setOnClickListener(v -> launchCamera(REQUEST_CODE_IMAGE1));
        image2View.setOnClickListener(v -> launchCamera(REQUEST_CODE_IMAGE2));
        matchButton.setOnClickListener(v ->{
            resultViewer.setText("Result will appear here");
            compareFaces();
        } );

        image1View = findViewById(R.id.image1);
        image2View = findViewById(R.id.image2);
        matchButton = findViewById(R.id.btn_match);
        resultViewer = findViewById(R.id.text_match_result);

    }
    private void initViews() {
        image1View = findViewById(R.id.image1);
        image2View = findViewById(R.id.image2);
        matchButton = findViewById(R.id.btn_match);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void launchCamera(int requestCode) {

        resultViewer.setText("Result will appear here");
        Intent intent = new Intent(this, PhotoCaptureActivity2.class);
        startActivityForResult(intent, requestCode);
    }


    private void loadBitmaps() {
        bitmap1 = LoadImageFromFile.loadBitmapFromFile(this, "full_photo_1748932015642.jpg"); // full_photo_1748864348734.jpg
        bitmap2 = LoadImageFromFile.loadBitmapFromFile(this, "full_photo_1748925453576.jpg"); //"3faces.jpg"

        image1View.setImageBitmap(bitmap1);
        image2View.setImageBitmap(bitmap2);
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
        try {
            progressBar.setVisibility(View.VISIBLE);

            FaceRecognizerHelper faceRecognizerHelper = new FaceRecognizerHelper(this);

            Mat detectedModelSuitedFrame1 = faceRecognizerHelper.makeFrameSuitToTheModel(bitmap1);
            Mat detectedModelSuitedFrame2 = faceRecognizerHelper.makeFrameSuitToTheModel(bitmap2);

            Mat faceCorrdinatesForImage1 = faceRecognizerHelper.detectFace(detectedModelSuitedFrame1);
            Mat faceCorrdinatesForImage2 = faceRecognizerHelper.detectFace( detectedModelSuitedFrame2);


            int faceCountInFrame1 = faceRecognizerHelper.numOfFaces(faceCorrdinatesForImage1);
            int faceCountInFrame2 = faceRecognizerHelper.numOfFaces(faceCorrdinatesForImage2);
            if (!isValidSingleFacePair(faceCountInFrame1, faceCountInFrame2)) {
                return;
            }

            Mat feature1 = faceRecognizerHelper.getFaceFeatureMatrix(detectedModelSuitedFrame1,faceCorrdinatesForImage1);
            Mat feature2 = faceRecognizerHelper.getFaceFeatureMatrix(detectedModelSuitedFrame2,faceCorrdinatesForImage2);

            double similarity = faceRecognizerHelper.compareFeatures(feature1, feature2);
            boolean isMatch = faceRecognizerHelper.isFaceMatched(feature1, feature2, MATCH_THRESHOLD);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                @SuppressLint("DefaultLocale")
                String result = String.format("Similarity: %.2f%%\nMatch: %s",
                        similarity * 100, isMatch ? "YES ✅" : "NO ❌");
                progressBar.setVisibility(View.INVISIBLE);
                resultViewer.setText(result);

                Log.i(TAG, result);
            }, 2000); // 2000 milliseconds = 2 seconds

        } catch (Exception e) {
            Log.e(TAG, "Face match error", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private boolean isValidSingleFacePair(int faceCount1, int faceCount2) {
        if (faceCount1 == 0 && faceCount2 == 0) {
            Toast.makeText(this, "No face detected in both frames", Toast.LENGTH_LONG).show();
            return false;
        } else if (faceCount1 > 1 && faceCount2 > 1) {
            Toast.makeText(this, "Multiple faces detected in both frames", Toast.LENGTH_LONG).show();
            return false;
        } else if (faceCount1 > 1) {
            Toast.makeText(this, "Multiple faces detected in frame 1\nNumber of faces: " + faceCount1, Toast.LENGTH_LONG).show();
            return false;
        } else if (faceCount2 > 1) {
            Toast.makeText(this, "Multiple faces detected in frame 2\nNumber of faces: " + faceCount2, Toast.LENGTH_LONG).show();
            return false;
        } else if (faceCount1 == 0) {
            Toast.makeText(this, "No face detected in frame 1", Toast.LENGTH_LONG).show();
            return false;
        } else if (faceCount2 == 0) {
            Toast.makeText(this, "No face detected in frame 2", Toast.LENGTH_LONG).show();
            return false;
        }

        return true; // Exactly one face in each frame
    }


}
