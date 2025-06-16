package com.kit.fingerprintcapture.viewmodel;

import android.content.Context;
import android.util.Log;
import android.view.View;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.handlers.IFingerMatcher;
import com.kit.fingerprintcapture.handlers.MorphoMatchingHandler;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.model.FingerprintCaptureItem;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.utils.FingerprintUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FingerprintCaptureViewModel extends ViewModel {


    String TAG = "FingerprintCaptureViewmodel";


    public final MutableLiveData<List<FingerprintCaptureItem>> fingerprintList = new MutableLiveData<>();
    private final MutableLiveData<FingerprintCaptureItem> currentFingerprint = new MutableLiveData<>();
    private final MutableLiveData<Boolean> captureStarted = new MutableLiveData<>(false);
    private final MutableLiveData<String> fingerprintStatusMessage = new MutableLiveData<>();
    private final ExecutorService captureExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService startCaptureExecutor = Executors.newSingleThreadExecutor();

    private boolean isDummyDevice = false;

    public IDeviceManager getmDeviceManager() {
        return mDeviceManager;
    }

    public void setmDeviceManager(IDeviceManager mDeviceManager) {
        this.mDeviceManager = mDeviceManager;
    }

    private IDeviceManager mDeviceManager;

    public boolean isDummyDevice() {
        return isDummyDevice;
    }

    public void setDummyDevice(boolean dummyDevice) {
        isDummyDevice = dummyDevice;
    }

    private FingerprintCaptureHandler captureHandler;

    private IDeviceManager iDeviceManager;

    private IFingerMatcher fingerMatcher;
    private MorphoMatchingHandler matchHandler;

//    public void init(View view, Context context) {
//        List<FingerprintCaptureItem> list = FingerprintUtils.generateAllFingerprints(view);
//        fingerprintList.setValue(list);
//
//        captureHandler = new FingerprintCaptureHandler(context, list);
//        captureExecutor.submit(captureHandler);
//
//        matchHandler = new MorphoMatchingHandler(context);
//        currentFingerprint.setValue(captureHandler.getFingerprintByID(FingerprintID.RIGHT_THUMB));
//    }

    public LiveData<List<FingerprintCaptureItem>> getFingerprintList() {
        return fingerprintList;
    }

    public LiveData<FingerprintCaptureItem> getCurrentFingerprint() {
        return currentFingerprint;
    }



    public void setFingerprintData(FingerprintCaptureItem curentItem , long score , byte[] fpData){

//        FingerprintCaptureItem fingerprintCaptureItem = getFingerprintByID(currentFingerprintID);
//        fingerprintCaptureItem.getFingerprintData().setFingerprintId(id);
        curentItem.getFingerprintData().setFingerprintData(fpData);
        curentItem.getFingerprintData().setQualityScore(score);

    }


    public LiveData<String> getStatusMessage() {
        return fingerprintStatusMessage;
    }

    public void startCapture(IDeviceManager deviceManager) {
        captureStarted.setValue(true);
        startCaptureExecutor.submit(() -> {
            deviceManager.startCapture();
        });
    }

    public void stopCapture() {
        captureHandler.stopCapture();
    }

    public void onCaptureFailed(FingerprintCaptureItem fp) {
        fp.setStatus(FingerprintStatus.NOT_CAPTURED);
        fingerprintStatusMessage.postValue("Capture Failed");
    }

    public void onCaptureFinished(FingerprintCaptureItem fp, byte[] wsqData, int score) {
        fp.getFingerprintData().setFingerprintData(wsqData);
        fp.getFingerprintData().setQualityScore(score);
        fp.setStatus(FingerprintStatus.CAPTURED);
        fingerprintStatusMessage.postValue("Capture Completed");
    }

    public void shutdownExecutors() {
        captureExecutor.shutdownNow();
        startCaptureExecutor.shutdownNow();
    }

}
