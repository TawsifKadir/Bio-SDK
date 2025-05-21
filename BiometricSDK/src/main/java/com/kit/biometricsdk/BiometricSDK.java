package com.kit.biometricsdk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
import com.kit.photocapture.activity.PhotoCaptureActivity2;
import com.kit.photocapture.util.Utility;
import com.kit.photocapture.activity.PhotoCaptureActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



public class BiometricSDK extends AppCompatActivity {
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
                Bitmap testBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.sample_face);

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
}