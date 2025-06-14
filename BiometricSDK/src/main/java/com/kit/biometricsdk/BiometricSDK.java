package com.kit.biometricsdk;

import android.content.Intent;
import android.graphics.Bitmap;
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

import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.photocapture.presentation.activity.PhotoCaptureActivity2;
import com.kit.photocapture.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



public class BiometricSDK extends AppCompatActivity {
    private Button FRButton;
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
        FRButton = findViewById(R.id.FRButton);

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

        FRButton.setOnClickListener(v -> {
//            try {
//                Intent nowIntent = new Intent(BiometricSDK.this,com.kit.photocapture.test.FaceRecognitionTest.class);
//                startActivityForResult(nowIntent,3);
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error initializing FaceMesh: ", e);
//            }
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
        }else if (requestCode == 3) {
            Log.d(TAG, "Returned from fingerprint capture");

            if (resultCode == RESULT_OK && data != null) {
                for (FingerprintID fid : FingerprintID.values()) {
                    FingerprintData fingerprintData = data.getParcelableExtra(fid.getName());
                    if (fingerprintData != null) {
                        logFingerprintData(fid.getName(), fingerprintData);
                    } else {
                        Log.d(TAG, "No data found for: " + fid.getName());
                    }
                }
            } else {
                Log.d(TAG, "Fingerprint capture was canceled or no data returned.");
            }
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
    private void logFingerprintData(String label, FingerprintData data) {
        Log.d(TAG, "------------ " + label + " ------------");

        if (data.getFingerprintId() != null) {
            Log.d(TAG, "ID: " + data.getFingerprintId().getName());
        } else {
            Log.d(TAG, "ID: null");
        }

        if (data.getFingerprintData() != null) {
            Log.d(TAG, "Raw fingerprint byte  " + data.getFingerprintData());
        } else {
            Log.d(TAG, "Raw fingerprint data: null");
        }

        Log.d(TAG, "Quality Score: " + data.getQualityScore());

        if (data.getIsoTemplate() != null) {
            byte[] iso = data.getIsoTemplate().getIsoTemplate();
            Log.d(TAG, "ISO Template Size: " + data.getIsoTemplate().getIsoTemplateSize());
            Log.d(TAG, "ISO Template First 20 Bytes: " + bytesToHex(iso, 20));
        } else {
            Log.d(TAG, "ISO Template: null");
        }
    }

    private String bytesToHex(byte[] bytes, int limit) {
        if (bytes == null) return "null";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(bytes.length, limit); i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }


}