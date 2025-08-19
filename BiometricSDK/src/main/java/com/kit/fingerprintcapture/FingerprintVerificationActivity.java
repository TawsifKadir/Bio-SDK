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
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.BaseActivityArr;
import com.kit.fingerprintcapture.utils.FingerprintsManager;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.kit.fingerprintcapture.utils.TemplateConverter;
import com.localafis.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FingerprintVerificationActivity extends BaseActivityArr implements DeviceDataCallback {


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
    private View loadingOverlay;
    private TextView loadingText;
    private volatile boolean templatesReady = false;
    private volatile boolean deviceReady = false;
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
        loadingText = findViewById(R.id.loadingText);
        loadingOverlay = findViewById(R.id.loadingOverlay);

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

        findViewById(android.R.id.content).post(this::startAsyncInitialization);
//
//        showLoading();
//
//        executorService.submit(() -> {
//            fingerprintsManager.buildEnumeratorFingerprintTemplates();
//            runOnUiThread(this::hideLoading);
//
//            Log.d(TAG, "Loaded enumeratorFingers from cache. Count: " + fingerprintsManager.getEnumeratorFingers().size());
//            for(FingerprintData fd: fingerprintsManager.getEnumeratorFingers())
//            {
//                Log.d(TAG, "Finger: \n" + fd.toString());
//            }
//            for(FingerprintTemplate fd: fingerprintsManager.getEnumeratorTemplates().values())
//            {
//                Log.d(TAG, "Finger: \n" + fd.toString().length());
//            }
//        });
    }


    @Override
    public void onResume(){
//        enableCapturing();
//
//        try {
//            long result = mDeviceManager.initDevice();
//            if(BuildConfig.isDebug){
//                Log.d(TAG, "initDevice() returned : " + result);
//            }
//            if(result!=0){
//                AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
//                dlgAlert.setMessage("Fingerprint device initialization failed with error : "+result);
//                dlgAlert.setTitle("Fingerprint SDK");
//                dlgAlert.setPositiveButton("OK",
//                        new DialogInterface.OnClickListener() {
//                            public void onClick(DialogInterface dialog,int whichButton){
//                                finish();
//                                return;
//                            }
//                        }
//                );
//                dlgAlert.setCancelable(false);
//                dlgAlert.create().show();
//            }
//        }catch(Throwable t){
//
//            t.printStackTrace();
//
//        }
//
//        try{
//            if(mDeviceManager.isPermissionAcquired()){
//                long result = mDeviceManager.openDevice();
//                if(result!=0) {
//                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
//                    dlgAlert.setMessage("Fingerprint device open failed with error : " + result);
//                    dlgAlert.setTitle("Fingerprint SDK");
//                    dlgAlert.setPositiveButton("OK",
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int whichButton) {
//                                    finish();
//                                }
//                            }
//                    );
//                    dlgAlert.setCancelable(false);
//                    dlgAlert.create().show();
//                }
//            }
//        }catch(Exception exc){
//
//        }
        super.onResume();
    }

    private void startAsyncInitialization() {
        // Templates
        loadingText.setText("Preparing fingerprint templates...");
        executorService.submit(() -> {
            try {
                fingerprintsManager.buildEnumeratorFingerprintTemplates();
                Log.d(TAG, "Templates built: " + fingerprintsManager.getEnumeratorTemplates().size());
                templatesReady = true;
                checkReadyAndUnlockUI();
            } catch (Throwable t) {
                Log.e(TAG, "Template build failed", t);
                showError("Failed to prepare templates.");
            }
        });

        // Device init/open
        runOnUiThread(() -> {
            long init = mDeviceManager.initDevice();
            if (init != 0) {
                showError("Device initialization failed: " + init);
                return;
            }
            long open = mDeviceManager.openDevice();
            if (open != 0) {
                showError("Device open failed: " + open);
                return;
            }
            deviceReady = true;
            checkReadyAndUnlockUI();
        });
    }

    /**
     * When both templates + device are ready, unlock UI
     */
    private void checkReadyAndUnlockUI() {
        if (templatesReady && deviceReady) {
            runOnUiThread(() -> {
                loadingOverlay.setVisibility(View.GONE);
                enableCapturing();
            });
        }
    }


    private void showError(String msg) {
        runOnUiThread(() -> {
            fingerprintText.setText(msg);
            loadingOverlay.setVisibility(View.GONE);
            disableButtonControls();
        });
    }

    @Override
    public void onPause() {
        mDeviceManager.closeDevice();
        hideLoading();
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
                    fingerprintsManager.getEnumeratorTemplates(),
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
    @Override
    public void onFingerprintData(byte[] imgData, int width, int height, int score, long result) {
        if (imgData != null && width > 0 && height > 0) {


            currentFingerprintTemplate = new FingerprintTemplate();
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
