package com.kit.fingerprintcapture.handlers;

import android.util.Log;
import android.view.View;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.callback.FingerprintCaptureCallback;
import com.kit.fingerprintcapture.model.Fingerprint;
import com.kit.fingerprintcapture.model.FingerprintID;

import java.util.ArrayList;
import java.util.concurrent.Callable;


public class FingerprintCaptureHandler implements Callable<Void>, View.OnClickListener{


    private Object syncObject;
    private ArrayList<Fingerprint> fingerPrintList;
    private FingerprintID currentFingerprintID;

    private FingerprintCaptureCallback captureCallback;

    private boolean startCapture=false;
    private boolean exitCapture=false;

    public boolean autoCaptureOn;
    public FingerprintCaptureHandler(FingerprintCaptureCallback captureCallback , ArrayList<Fingerprint> fingerPrintList) {
        syncObject = new Object();
        currentFingerprintID = FingerprintID.RIGHT_THUMB;
        startCapture = false;
        exitCapture=false;
        this.autoCaptureOn = false;
        this.fingerPrintList = fingerPrintList;
        this.captureCallback = captureCallback;
    }

   /// @Override
    public void run() {
//        while(true){
//            if(exitCapture){break;}
//            while(!startCapture){
//                synchronized (syncObject){
//                    try {
//                        syncObject.wait();
//                    }catch(Exception exc){
//
//                    }
//                }
//            }
//
//            startCapture=false;
//            if(this.autoCaptureOn){
//                if(BuildConfig.isDebug) {
//                    Log.d("FaisalActivity", ">>>>> Going to capture fingerprint >>>> " + currentFingerprintID);
//                }
//                Fingerprint fp = getFingerprintByID(currentFingerprintID);
//                captureCallback.onCaptureStart(fp);
//            }
//        }
    }

    public void setFingerprintData(FingerprintID id , long score , byte[] fpData){
        if(BuildConfig.isDebug) {
            Log.d("FingerprintCapture", ">>>>> Entered setFingerprintData >>>> ");
            Log.d("FingerprintCapture", ">>>>> Fingerprint Data Size : " + fpData.length);
        }
        Fingerprint fingerprint = getFingerprintByID(currentFingerprintID);
        fingerprint.getFingerprintData().setFingerprintId(id);
        fingerprint.getFingerprintData().setFingerprintData(fpData);
        fingerprint.getFingerprintData().setQualityScore(score);

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
        this.currentFingerprintID = getNextID();
        if(BuildConfig.isDebug) {
            Log.d("FaisalActivity", ">>>>> Next capture id is " + this.currentFingerprintID + " >>>>> ");
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
        ////this.currentFingerprintID = getNextID();
        startCapture = true;
        synchronized (syncObject){
            syncObject.notifyAll();
        }
    }
    public FingerprintID getNextID(){
        int nowID = this.currentFingerprintID.getID();
        int nextID = ((nowID+1))%11;

        if(nextID==0){
            nextID=1;
        }

        return FingerprintID.getFingerprintID(nextID);
    }

    public FingerprintID getPrevID(){
        int nowID = this.currentFingerprintID.getID();
        int prevID = ((nowID-1)+11)%11;

        return FingerprintID.getFingerprintID(prevID);
    }

    public Fingerprint getFingerprintByViewID(View v){
        for(Fingerprint fp:fingerPrintList){
            if(fp.getFingerprintUI().getFingerprintBtn().getId()==v.getId()){
                return fp;
            }
        }
        return null;
    }

    public Fingerprint getFingerprintByID(FingerprintID fpID){
        for(Fingerprint fp:fingerPrintList){
            if(fp.getFingerprintID()==fpID){
                return fp;
            }
        }
        return null;
    }

    public ArrayList<Fingerprint> getFingerPrintList(){
        return this.fingerPrintList;
    }

    @Override
    public void onClick(View v) {

            captureCallback.onCaptureStop(getFingerprintByID(this.currentFingerprintID));
            Fingerprint fp = getFingerprintByViewID(v);
            this.currentFingerprintID = fp.getFingerprintID();
            captureCallback.onCaptureStart(getFingerprintByID(this.currentFingerprintID));
    }


    public void setCurrentFingerprintID(FingerprintID currentFingerprintID) {
        this.currentFingerprintID = currentFingerprintID;
    }

    @Override
    public Void call() throws Exception {

        while(true){

            if(exitCapture){break;}

            while(!startCapture){
                synchronized (syncObject){
                    try {
                        syncObject.wait();
                    }catch(Exception exc){

                    }
                }
            }

            startCapture=false;

            if(this.autoCaptureOn){
                if(BuildConfig.isDebug) {
                    Log.d("FaisalActivity", ">>>>> Going to capture fingerprint >>>> " + currentFingerprintID);
                }
                Fingerprint fp = getFingerprintByID(currentFingerprintID);
                captureCallback.onCaptureStart(fp);
            }
        }

        return Void.TYPE.newInstance();
    }
}
