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

import com.kit.photocapture.Utility;

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

        mPhotoCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nowIntent = new Intent(BiometricSDK.this,com.kit.photocapture.PhotoCaptureActivity.class);
                startActivityForResult(nowIntent,2);
            }
        });

        mFpCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent nowIntent = new Intent(BiometricSDK.this,com.kit.fingerprintcapture.FingerprintCaptureActivity.class);
                startActivityForResult(nowIntent,3);
            }
        });

        mCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
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