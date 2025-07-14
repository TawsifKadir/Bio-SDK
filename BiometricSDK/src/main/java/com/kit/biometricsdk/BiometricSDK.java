package com.kit.biometricsdk;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kit.fingerprintcapture.FingerprintCaptureActivity;
import com.kit.fingerprintcapture.FingerprintCaptureActivity2;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.photocapture.presentation.activity.PhotoCaptureActivity;
import com.kit.photocapture.util.Utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;




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
            Intent nowIntent = new Intent(BiometricSDK.this, PhotoCaptureActivity.class);
            startActivityForResult(nowIntent,2);
        });

        mFpCaptureBtn.setOnClickListener(v -> {
            Intent nowIntent = new Intent(BiometricSDK.this, FingerprintCaptureActivity.class);
            startActivityForResult(nowIntent,3);
        });


        mCloseBtn.setOnClickListener(v -> {
            Intent nowIntent = new Intent(BiometricSDK.this, FingerprintCaptureActivity2.class);
            startActivityForResult(nowIntent, 4);
        });

//
//        mCloseBtn.setOnClickListener(v -> {
//            // Create intent to navigate to FingerprintCaptureActivity2
//            Intent nowIntent = new Intent(BiometricSDK.this, FingerprintCaptureActivity2.class);
//
//            // Add mock userType
//            nowIntent.putExtra("userType", "TEST_USER");
//
//            // Add mock fingerprint data for RIGHT_THUMB
//            FingerprintData mockData = new FingerprintData(
//                    FingerprintID.RIGHT_THUMB,
//                    new byte[]{1, 2, 3, 4, 5},  // Mock fingerprint bytes
//                    85,                         // Mock quality score
//                    new byte[]{9, 8, 7, 6, 5}   // Mock ISO template
//            );
//            nowIntent.putExtra(FingerprintID.RIGHT_THUMB.getName(), mockData);
//
//            // If needed, add noFingerprint info (simulate no finger)
//            nowIntent.putExtra("noFingerprint", false);
//            nowIntent.putExtra("noFingerprintReasonID", -1);
//            nowIntent.putExtra("noFingerprintReasonText", "");
//
//            // Start the activity expecting result
//            startActivityForResult(nowIntent, 4);
//
//            Log.d(TAG, "Started FingerprintCaptureActivity2 with mock data");
//        });
//


//        mCloseBtn.setOnClickListener(v -> {
//            Intent nowIntent = new Intent(BiometricSDK.this, FingerprintCaptureActivity2.class);
//            startActivityForResult(nowIntent,4);
//
////            try {
////                FaceComparisionTest.compareTwoFaces(getApplicationContext(), R.drawable.sample_face, R.drawable.demo_ronaldo, "Ronaldo");
////                FaceComparisionTest.compareTwoFaces(getApplicationContext(), R.drawable.sample_face, R.drawable.demo_messi, "Messi");
////
////            } catch (Exception e) {
////                Log.e(TAG, "Error initializing FaceMesh: ", e);
////            }
//        });

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
        }
        else if(requestCode==3){
            Log.d(TAG, "Returned from fingerprint capture");
            if (resultCode == RESULT_OK && data != null) {
                if (data.hasExtra("noFingerprint")) {
                    boolean hasException = data.getBooleanExtra("noFingerprint", false);
                    if (hasException) {
                        int reasonId = data.getIntExtra("noFingerprintReasonID", -1);
                        String reasonText = data.getStringExtra("noFingerprintReasonText");
                    }
                }
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
        else if( requestCode == 4){
            Log.d(TAG, "Returned from fingerprint capture");



            if ( resultCode == RESULT_OK ) {

                Log.d(TAG, "Request Code is ok ");
                if( data != null)
                {
                    Log.d(TAG, "Intent Data is not null");

                    // 1. Log Intent extras
                    logIntentExtras(data);

                    // 2. Get cached fingerprint data
                    List<FingerprintData> cachedFingerprints = FingerprintCache.getInstance().getFingerList();

                    // 3. Log all fingerprint fields using helper
                    if (cachedFingerprints != null && !cachedFingerprints.isEmpty()) {
                        logCachedFingerprints(cachedFingerprints);
                    } else {
                        Log.d(TAG, "No fingerprint data found in cache.");
                    }

                }else {

                    Log.d(TAG, "Intent Data is null");
                }
                // 4. Clear the cache after logging
                FingerprintCache.getInstance().clear();
                Log.d(TAG, "Fingerprint cache cleared.");
            }
//            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
//
//
//            }
//            else{
//                Log.d(TAG, "SDK not suported");
//            }

        }
    }


    private void logIntentExtras(Intent data) {
        Bundle extras = data.getExtras();
        if (extras != null) {
            Log.d(TAG, "----- Received Intent Extras -----");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d(TAG, key + " = " + (value != null ? value.toString() : "null"));
            }
            Log.d(TAG, "----------------------------------");
        } else {
            Log.d(TAG, "No extras in received intent.");
        }
    }

    private void logCachedFingerprints(List<FingerprintData> fingerprintList) {
        Log.d(TAG, "----- Cached Fingerprint Data -----");

        for (FingerprintData dataItem : fingerprintList) {
            String idName = (dataItem.getFingerprintId() != null)
                    ? dataItem.getFingerprintId().getName()
                    : "Unknown";

            int dataSize = (dataItem.getFingerprintData() != null)
                    ? dataItem.getFingerprintData().length
                    : 0;

            int templateSize = (dataItem.getIsoTemplate() != null)
                    ? dataItem.getIsoTemplate().length
                    : 0;

            Log.d(TAG, "ID: " + idName);
            Log.d(TAG, "Fingerprint Data Size: " + dataSize + " bytes");
            Log.d(TAG, "ISO Template Size: " + templateSize + " bytes");
            Log.d(TAG, "Quality Score: " + dataItem.getQualityScore());
            Log.d(TAG, "-----------------------------------");
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
            byte[] iso = data.getIsoTemplate();
            Log.d(TAG, "ISO Template Size: " + iso.length);
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