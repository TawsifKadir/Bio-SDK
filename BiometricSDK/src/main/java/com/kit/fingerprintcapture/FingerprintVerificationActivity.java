package com.kit.fingerprintcapture;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.common.CustomToastHandler;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.handlers.FingerprintMatchingHandler;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.manager.MorphoDeviceManager;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.BaseActivityArr;
import com.kit.fingerprintcapture.utils.FingerprintsManager;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FingerprintVerificationActivity extends BaseActivityArr implements DeviceDataCallback {


    public static String wsqtest= "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"
    private static final String TAG = "FingerprintVerification";
    public static final String KEY_VERIFICATION_RESULT = "Verification";

    private TextView fingerprintText;
    private ImageView fingerprintImage;
//    private MaterialButton verifyBtn;
    private MaterialButton captureButton;
    private MaterialButton backButton;

    private MaterialButton btProceed;

    private ExecutorService executorService;
    private ExecutorService captureExecutor;

    private IDeviceManager mDeviceManager;
    private FingerprintMatchingHandler mfpMatchHandler;

    private FingerprintCaptureHandler mfpCaptureHandler;
    private FingerprintTemplate currentFingerprintTemplate;
    private final FingerprintsManager fingerprintsManager = new FingerprintsManager();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_fingerprint_verification2);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        fingerprintText = findViewById(R.id.fingerprint_text);
        fingerprintImage = findViewById(R.id.fingerprint_image);
//        verifyBtn = findViewById(R.id.veifyBtn);
        captureButton = findViewById(R.id.fpCaptureBtn);
        backButton = findViewById(R.id.btBackButton);
        btProceed=  findViewById(R.id.btproceed);

        executorService = Executors.newSingleThreadExecutor();
        captureExecutor = Executors.newSingleThreadExecutor();

        mDeviceManager = new MorphoDeviceManager(this, this);
        mfpMatchHandler = new FingerprintMatchingHandler(this);
        //mfpCaptureHandler = new FingerprintCaptureHandler(this, new ArrayList<>());

        captureButton.setOnClickListener(v -> startFingerprintCapture());
//        verifyBtn.setOnClickListener(v -> startVerification());

        backButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_VERIFICATION_RESULT, false);
            setResult(RESULT_OK, resultIntent);
           // disableControls();
            finish();
        });
        btProceed.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(KEY_VERIFICATION_RESULT, true);
            setResult(RESULT_OK, resultIntent);

            finish();


        });
    }


    @Override
    public void onResume(){
        enableCapturing();

        try {
            long result = mDeviceManager.initDevice();
            if(BuildConfig.isDebug){
                Log.d(TAG, "initDevice() returned : " + result);
            }
            if(result!=0){
                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                dlgAlert.setMessage("Fingerprint device initialization failed with error : "+result);
                dlgAlert.setTitle("Fingerprint SDK");
                dlgAlert.setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int whichButton){
                                finish();
                                return;
                            }
                        }
                );
                dlgAlert.setCancelable(false);
                dlgAlert.create().show();
            }
        }catch(Throwable t){

            t.printStackTrace();

        }

        try{
            if(mDeviceManager.isPermissionAcquired()){
                long result = mDeviceManager.openDevice();
                if(result!=0) {
                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
                    dlgAlert.setMessage("Fingerprint device open failed with error : " + result);
                    dlgAlert.setTitle("Fingerprint SDK");
                    dlgAlert.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    finish();
                                }
                            }
                    );
                    dlgAlert.setCancelable(false);
                    dlgAlert.create().show();
                }
            }
        }catch(Exception exc){

        }
        super.onResume();
    }

    @Override
    public void onPause() {
        mDeviceManager.closeDevice();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        shutdownExecutors();
        cleanupDevice();
        super.onDestroy();
    }

    private void startFingerprintCapture() {
        disableButtonControls();
        captureExecutor.submit(() -> {
            try {
                mDeviceManager.startCapture();
               // runOnUiThread(() -> Toast.makeText(this, "Capture started", Toast.LENGTH_SHORT).show());
            } catch (Throwable t) {
                Log.e(TAG, "Capture start error", t);
                showErrorDialogOnUi("Failed to start capture");
                enableCapturing();
            }
        });
    }

    public void startVerification() {
        executorService.submit(() -> {
            List<MatchResult> matchList = new ArrayList<>();
            mfpMatchHandler.verifyFingerPrint2(
                    FingerprintID.RIGHT_THUMB.getID(),
                    currentFingerprintTemplate,
                    new ArrayList<>(fingerprintsManager.getEnumeratorTemplates().values()),
                    matchList
            );

            Log.d(TAG, "startVerification() called");

            runOnUiThread(() -> {
                if (!matchList.isEmpty()) {
                    fingerprintText.setText("Fingerprint Mathed");
                    Log.d(TAG, "startVerification() called matched");
                    CustomToastHandler.showErrorToast(this, "Successfully matched!!");
                    enableProceedButton();
                } else {
                    fingerprintText.setText("Fingerprint Not Mathed");
                    Log.d(TAG, "startVerification() called not matched");
                    CustomToastHandler.showErrorToast(this, "Did not match!!");
                    enableCapturing();
                }

            });
        });
    }



    public static byte[] base64ToByteArray(String base64String) {
        // Validate input
        if (base64String == null) {
            throw new IllegalArgumentException("Input string cannot be null");
        }
        if (base64String.trim().isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be empty");
        }

        try {
            byte[] result = Base64.decode(base64String, Base64.DEFAULT);

            // Additional validation for decode result
            if (result == null || result.length == 0) {
                throw new IllegalArgumentException("Decoding resulted in empty byte array - possibly invalid input");
            }

            return result;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Input is not a valid Base64 string", e);
        }
    }

    @Override
    public void onFingerprintData(byte[] imgData, int width, int height, int score, long result) {
        if (imgData != null && width > 0 && height > 0) {

            byte[] test = base64ToByteArray(wsqtest);

            currentFingerprintTemplate =new FingerprintTemplate();
            currentFingerprintTemplate.dpi(500).create(imgData, width, height);
            runOnUiThread(() -> {
                fingerprintImage.setImageBitmap(ImageProc.toGrayscale(imgData, width, height));
                startVerification();
//                enableVerification();
            });
        }
    }

    @Override
    public void onFingerprintPreview(Bitmap img, int width, int height) {
        runOnUiThread(() -> fingerprintImage.setImageBitmap(img));
    }

    @Override
    public void onCaptureCmd(String cmd) {
        runOnUiThread(() -> fingerprintText.setText(cmd));
    }

    @Override
    public void onCaptureError(String errorMsg) {
        runOnUiThread(() -> {
            fingerprintImage.setImageBitmap(Bitmap.createBitmap(248, 448, Bitmap.Config.ARGB_8888));
            showErrorDialog(errorMsg);
            enableCapturing();
        });
    }

    private void showErrorDialogOnUi(String message) {
        runOnUiThread(() -> showErrorDialog(message));
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void disableButtonControls() {
        captureButton.setEnabled(false);
//        verifyBtn.setEnabled(false);
//        verifyBtn.setVisibility(View.GONE);
        btProceed.setEnabled(false);
      //  btProceed.setVisibility(View.GONE);
    }

//    @SuppressLint("SetTextI18n")
//    private void enableVerification() {
//        captureButton.setEnabled(false);
//        captureButton.setVisibility(View.GONE);
//        btProceed.setEnabled(false);
//        btProceed.setVisibility(View.GONE);
////        verifyBtn.setVisibility(View.VISIBLE);
////        verifyBtn.setEnabled(true);
//    }

    private void enableCapturing() {
        captureButton.setEnabled(true);
        captureButton.setVisibility(View.VISIBLE);
//        verifyBtn.setEnabled(false);
//        verifyBtn.setVisibility(View.GONE);
        btProceed.setEnabled(false);
//        btProceed.setVisibility(View.GONE);

    }

    private void enableProceedButton() {
        captureButton.setEnabled(true);
        captureButton.setVisibility(View.GONE);
//        verifyBtn.setEnabled(false);
//        verifyBtn.setVisibility(View.GONE);
        btProceed.setVisibility(View.VISIBLE);
        btProceed.setEnabled(true);


    }

    private void cleanupDevice() {
        try {
            mDeviceManager.closeDevice();
            mDeviceManager.deInitDevice();
         //   mfpMatchHandler.setMatcher(null);
        } catch (Throwable t) {
            Log.e(TAG, "Cleanup error", t);
        }
    }


//    private void proceedToNext() {
//        finish();
//    }


    private void shutdownExecutors() {
        captureExecutor.shutdownNow();
        executorService.shutdownNow();
        try {
            captureExecutor.awaitTermination(1, TimeUnit.SECONDS);
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Executor shutdown error", e);
        }
    }
}
