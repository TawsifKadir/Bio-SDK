package com.kit.biometricsdk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.mediapipe.formats.proto.LandmarkProto;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.kit.Point3D;
import com.kit.photocapture.activity.PhotoCaptureActivity2;
import com.kit.photocapture.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BiometricSDK extends AppCompatActivity {


    List<Point3D> allLandMarks = new ArrayList<>();

    private Button mCloseBtn;
    private Button mPhotoCaptureBtn;
    private Button mFpCaptureBtn;
    private ImageView mPhotoView;

    private String TAG = "MAIN_ACTIVITY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_biometric_sdk);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mFpCaptureBtn = findViewById(R.id.fpCaptureBtn);
        mPhotoCaptureBtn = findViewById(R.id.photoCaptureBtn);
        mCloseBtn = findViewById(R.id.closeBtn);

        mPhotoView = findViewById(R.id.photoView);

        mPhotoCaptureBtn.setOnClickListener(v -> {
            Intent nowIntent = new Intent(BiometricSDK.this, PhotoCaptureActivity2.class);
            startActivityForResult(nowIntent,2);
        });

        mFpCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nowIntent = new Intent(BiometricSDK.this,com.kit.fingerprintcapture.FingerprintCaptureActivity.class);
                startActivityForResult(nowIntent,3);
            }
        });

        mCloseBtn.setOnClickListener(v -> {
            try {
                Bitmap testBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.rf2_rec);

                FaceMeshOptions options = FaceMeshOptions.builder()
                        .setStaticImageMode(true)
                        .setMaxNumFaces(1)
                        .setRefineLandmarks(true)
                        .build();

                FaceMesh faceMesh = new FaceMesh(this, options);

                faceMesh.setErrorListener((message, e) -> {
                    Log.e(TAG, "FaceMesh error: " + message, e);
                });

                faceMesh.setResultListener(result -> {
                    if (result == null || result.multiFaceLandmarks().isEmpty()) {
                        Log.d(TAG, "❌ No landmarks detected.");
                    } else {
                        LandmarkProto.NormalizedLandmarkList landmarkList = result.multiFaceLandmarks().get(0);
                        int count = landmarkList.getLandmarkCount();
                        Log.d(TAG, "✅ Landmark count: " + count);

                        // Sample landmark indices to inspect
                        int[] sampleIndices = {0, 1, 33, 263};
                        for (int idx : sampleIndices) {
                            if (idx < count) {
                                LandmarkProto.NormalizedLandmark lm = landmarkList.getLandmark(idx);
                                Log.d(TAG, String.format("Landmark[%d] -> x: %.4f, y: %.4f, z: %.4f, visibility: %.4f, presence: %.4f",
                                        idx, lm.getX(), lm.getY(), lm.getZ(), lm.getVisibility(), lm.getPresence()));
                            }
                        }



                        allLandMarks.clear(); // Clear previous entries if needed

                        for (int idx = 0; idx < count; idx++) {
                            LandmarkProto.NormalizedLandmark lm = landmarkList.getLandmark(idx);
                            Point3D point = new Point3D(lm.getX(), lm.getY(), lm.getZ());
                            allLandMarks.add(point);
                        }
//
//                        // Log after collecting all landmarks
//                        for (int i = 0; i < allLandMarks.size(); i++) {
//                            Point3D pt = allLandMarks.get(i);
//                            Log.d(TAG, String.format("📍 AllLandmark[%d]: x=%.4f, y=%.4f, z=%.4f", i, pt.x, pt.y, pt.z));
//                        }

                        boolean leftEyeOpen = isLeftEyeOpen(allLandMarks);
                        boolean rightEyeOpen = isRightEyeOpen(allLandMarks);


                        Log.d(TAG, "👁 Left Eye Open? " + leftEyeOpen);
                        Log.d(TAG, "👁 Right Eye Open? " + rightEyeOpen);


                        // drawAndSaveLandmarks(testBitmap, allLandMarks);





                    }
                });


                faceMesh.send(testBitmap);  // ✅ THIS is the correct call, not `.send(image)`

            } catch (Exception e) {
                Log.e(TAG, "Error initializing FaceMesh: ", e);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==2){
            if(resultCode==RESULT_OK) {
                String uriStr = data.getStringExtra("IMAGE_URI");
                Log.d(TAG, "Received URI " + uriStr);
                if(uriStr!=null)
                {
                    Uri imgUri = Uri.parse(uriStr);
                    Bitmap nowBmp = Utility.getImageData(BiometricSDK.this,imgUri);
                    if(nowBmp!=null){
                        if(mPhotoView!=null){
                            mPhotoView.setImageBitmap(nowBmp);
                            byte[] nowData = convertBitmapToByteArray(nowBmp);
                            if(nowData!=null){
                                Log.d(TAG,"IMAGE SIZE IS : "+nowData.length);
                                Log.d(TAG,"IMAGE WIDTH IS : "+nowBmp.getWidth());
                                Log.d(TAG,"IMAGE HEIGHT IS : "+nowBmp.getHeight());

                            }else{
                                Log.e(TAG,"BITMAP TO BYTE CONVERSION ERROR ");
                            }

                        }
                    }else{
                        Log.e(TAG,"Error converting Uri to BMP");
                    }
                }else{
                    Log.e(TAG,"Received null uri from activity");
                }
            }else{
                Log.d(TAG, "Error occurred ");
            }
        }else if(requestCode==3){
            Log.d(TAG, "Returned from fingerprint capture");
        }
    }

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = null;
        try {
            stream = new ByteArrayOutputStream();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 100, stream);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }else{
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            }

            return stream.toByteArray();
        }finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    Log.e(TAG, "ByteArrayOutputStream was not closed");
                }
            }
        }
    }


    private void drawAndSaveLandmarks(Bitmap originalBitmap, List<Point3D> landmarks) {
        // Create a mutable copy
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // Paint for landmark dots
        Paint dotPaint = new Paint();
        dotPaint.setColor(Color.RED);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setAntiAlias(true);
        dotPaint.setStrokeWidth(6f);

        // Paint for text labels
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLUE);
        textPaint.setTextSize(5); // small size
        textPaint.setAntiAlias(true);

        // Draw each landmark with index
        for (int i = 0; i < landmarks.size(); i++) {
            Point3D point = landmarks.get(i);
            float x = (float) (point.x * originalBitmap.getWidth());
            float y = (float) (point.y * originalBitmap.getHeight());

            canvas.drawCircle(x, y, 1, dotPaint);
            canvas.drawText(String.valueOf(i), x + 2, y - 2, textPaint); // slight offset
        }

        // Save the result to app-private storage (safe and permission-free)
        try {
            File directory = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "FaceMesh");
            if (!directory.exists()) {
                boolean created = directory.mkdirs();
                if (!created) {
                    Log.e(TAG, "❌ Failed to create directory: " + directory.getAbsolutePath());
                    return;
                }
            }

            File outputFile = new File(directory, "dottedimage.jpg");
            try (FileOutputStream out = new FileOutputStream(outputFile)) {
                mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                Log.d(TAG, "✅ Image saved: " + outputFile.getAbsolutePath());
            }

        } catch (IOException e) {
            Log.e(TAG, "❌ Failed to save image", e);
        }
    }


    private boolean isEyeOpen(List<Point3D> landmarks, boolean isLeftEye) {
        // Use left or right eye indices based on MediaPipe reference
        int[] eyeIndices = isLeftEye
                ? new int[]{159, 145, 33, 133}  // Left Eye: Top, Bottom, Left, Right
                : new int[]{386, 374, 362, 263}; // Right Eye: Top, Bottom, Left, Right

        Point3D top = landmarks.get(eyeIndices[0]);
        Point3D bottom = landmarks.get(eyeIndices[1]);
        Point3D left = landmarks.get(eyeIndices[2]);
        Point3D right = landmarks.get(eyeIndices[3]);

        float verticalDist = getDistance(top, bottom);
        float horizontalDist = getDistance(left, right);

        float ear = verticalDist / horizontalDist;

        // You can tune this threshold based on your camera distance and lighting
        return ear > 0.20f;
    }

    private float getDistance(Point3D a, Point3D b) {
        float dx = (float) (a.x - b.x);
        float dy = (float) (a.y - b.y);
        return (float) Math.sqrt(dx * dx + dy * dy);
    }


    public boolean isLeftEyeOpen(List<Point3D> landmarks) {
        int[] vertical = {159, 145, 160, 144, 161, 163};
        int[] horizontal = {33, 133};

        float verticalDist = (
                getDistance(landmarks.get(vertical[0]), landmarks.get(vertical[1])) +
                        getDistance(landmarks.get(vertical[2]), landmarks.get(vertical[3])) +
                        getDistance(landmarks.get(vertical[4]), landmarks.get(vertical[5]))
        ) / 3.0f;

        float horizontalDist = getDistance(
                landmarks.get(horizontal[0]),
                landmarks.get(horizontal[1])
        );

        float ear = verticalDist / horizontalDist;

        return ear > 0.23f;
    }
    public boolean isRightEyeOpen(List<Point3D> landmarks) {
        int[] vertical = {386, 374, 387, 373, 388, 390};
        int[] horizontal = {362, 263};

        float verticalDist = (
                getDistance(landmarks.get(vertical[0]), landmarks.get(vertical[1])) +
                        getDistance(landmarks.get(vertical[2]), landmarks.get(vertical[3])) +
                        getDistance(landmarks.get(vertical[4]), landmarks.get(vertical[5]))
        ) / 3.0f;

        float horizontalDist = getDistance(
                landmarks.get(horizontal[0]),
                landmarks.get(horizontal[1])
        );

        float ear = verticalDist / horizontalDist;

        return ear > 0.23f;
    }


}