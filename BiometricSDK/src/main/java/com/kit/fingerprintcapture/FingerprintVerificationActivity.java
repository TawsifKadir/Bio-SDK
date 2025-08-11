package com.kit.fingerprintcapture;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.kit.biometricsdk.R;
import com.kit.common.CustomToastHandler;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.handlers.FingerprintMatchingHandler;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.manager.MorphoDeviceManager;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.BaseActivityArr;
import com.kit.fingerprintcapture.utils.FingerprintUtils;
import com.kit.fingerprintcapture.utils.FingerprintsManager;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

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


    private FingerprintTemplate currentFingerprintTemplate;
    private final FingerprintsManager fingerprintsManager = new FingerprintsManager();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_fingerprint_verification);
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
    protected void onResume() {
        super.onResume();
        enableCapturing();
        //disableButtonControls();
        initDevice();
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

    private void initDevice() {
        executorService.submit(() -> {
            try {
                long result = mDeviceManager.initDevice();
//                if (result != ErrorCodes.FPC_SUCCESS) {
//                    showErrorDialogOnUi("Device init failed: " + result);
//                    return;
//                }
//
//                result = mDeviceManager.openDevice();
//                if (result != ErrorCodes.FPC_SUCCESS) {
//                    showErrorDialogOnUi("Device open failed: " + result);
//                    return;
//                }

//                Matcher matcher = new Matcher();
//                matcher.setRotationToleranceInDegree(180);
//                mfpMatchHandler.setMatcher(matcher);

              //  runOnUiThread(this::enableCapturing);
            } catch (Throwable t) {
                Log.e(TAG, "Device init error", t);
                showErrorDialogOnUi("Device error: " + t.getMessage());
            }
        });
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
                    new ArrayList<>(fingerprintsManager.getTakenFingersTemplates().values()),
                    matchList
            );

            runOnUiThread(() -> {
                if (!matchList.isEmpty()) {
                    fingerprintText.setText("Fingerprint Mathed");
                    CustomToastHandler.showErrorToast(this, "Successfully matched!!");
                    enableProceedButton();
                } else {
                    fingerprintText.setText("Fingerprint Not Mathed");
                    CustomToastHandler.showErrorToast(this, "Did not match!!");
                    enableCapturing();
                }

            });
        });
    }

    @Override
    public void onFingerprintData(byte[] imgData, int width, int height, int score, long result) {
        if (imgData != null && width > 0 && height > 0) {
//            FingerprintData data = FingerprintUtils.imageToFingerprintDataModel(imgData, width, height, FingerprintID.LEFT_THUMB.getID());
//            currentFingerprintTemplate = new ISOTemplate(data.getIsoTemplate(), data.getIsoTemplate().length);

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
