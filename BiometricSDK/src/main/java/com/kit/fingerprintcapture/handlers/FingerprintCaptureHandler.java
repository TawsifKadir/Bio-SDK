package com.kit.fingerprintcapture.handlers;

import android.util.Log;
import android.view.View;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.callback.FingerprintCaptureCallback;
import com.kit.fingerprintcapture.model.FingerprintCaptureItem;
import com.kit.fingerprintcapture.model.FingerprintID;

import java.util.ArrayList;
import java.util.concurrent.Callable;


public class FingerprintCaptureHandler implements Callable<Void>, View.OnClickListener{

    public static final String TAG = "FingerprintCaptureHandler";
    private final Object syncObject;
    private final ArrayList<FingerprintCaptureItem> fingerPrintList;
    private FingerprintID currentFingerprintID;
    private final FingerprintCaptureCallback captureCallback;
    private boolean startCapture;
    private boolean exitCapture;

    public FingerprintCaptureHandler(FingerprintCaptureCallback captureCallback , ArrayList<FingerprintCaptureItem> fingerPrintList) {
        syncObject = new Object();
        currentFingerprintID = FingerprintID.RIGHT_THUMB;
        startCapture = false;
        exitCapture = false;
        this.fingerPrintList = fingerPrintList;
        this.captureCallback = captureCallback;
    }

    public void setFingerprintData(FingerprintID id , long score , byte[] fpData){
        if(BuildConfig.isDebug) {
            Log.d("FingerprintCapture", ">>>>> Entered setFingerprintData >>>> ");
            Log.d("FingerprintCapture", ">>>>> Fingerprint Data Size : " + fpData.length);
        }
        FingerprintCaptureItem fingerprintCaptureItem = getFingerprintByID(currentFingerprintID);
        fingerprintCaptureItem.getFingerprintData().setFingerprintId(id);
        fingerprintCaptureItem.getFingerprintData().setFingerprintData(fpData);
        fingerprintCaptureItem.getFingerprintData().setQualityScore(score);

    }
    public void startCapture(){
        startCapture = true;
        synchronized (syncObject){
            syncObject.notifyAll();
        }
    }

    public void stopCapture(){
        startCapture = false;
        synchronized (syncObject){
            syncObject.notifyAll();;
        }
    }


    public void exitCapture(){
        exitCapture = true;
        synchronized (syncObject){
            syncObject.notifyAll();;
        }
    }

    public void captureFinished(){
        if(BuildConfig.isDebug) {
            Log.d("FaisalActivity", ">>>>> Entered captureFinished >>>> ");
        }

        startCapture = true;
        synchronized (syncObject){
            syncObject.notifyAll();
        }
    }
    public void captureFailed(){
        if(BuildConfig.isDebug) {
            Log.d("FaisalActivity", ">>>>> Entered captureFailed >>>> ");
        }

        startCapture = true;
        synchronized (syncObject){
            syncObject.notifyAll();
        }
    }
    public FingerprintCaptureItem getFingerprintByViewID(View v){
        for(FingerprintCaptureItem fp:fingerPrintList){
            if(fp.getFingerprintUI().getFingerprintBtn().getId() == v.getId()){
                return fp;
            }
        }
        return null;
    }

    public FingerprintCaptureItem getFingerprintByID(FingerprintID fpID){
        for(FingerprintCaptureItem fp : fingerPrintList){
            if(fp.getFingerprintID() == fpID){
                return fp;
            }
        }
        return null;
    }

    public ArrayList<FingerprintCaptureItem> getFingerPrintList(){
        return this.fingerPrintList;
    }

    @Override
    public void onClick(View v) {
        captureCallback.onCaptureStop(getFingerprintByID(this.currentFingerprintID));
        FingerprintCaptureItem fp = getFingerprintByViewID(v);
        this.currentFingerprintID = fp.getFingerprintID();
        captureCallback.onCaptureStart(getFingerprintByID(this.currentFingerprintID));
    }
    public void setCurrentFingerprintID(FingerprintID currentFingerprintID) {
        this.currentFingerprintID = currentFingerprintID;
    }

    @Override
    public Void call() throws Exception {

        while (!exitCapture) {

            while (!startCapture) {
                synchronized (syncObject) {
                    try {
                        syncObject.wait();
                    } catch (Exception exc) {
                        Log.d(TAG, "Error while capturing " + exc);
                    }
                }
            }

            startCapture = false;
        }

        return Void.TYPE.newInstance();
    }
}
