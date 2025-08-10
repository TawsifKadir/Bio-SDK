package com.kit.fingerprintcapture;

import androidx.appcompat.app.AlertDialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.Bundle;

import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.common.CustomToastHandler;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.callback.FingerprintCaptureCallback;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.handlers.FingerprintMatchingHandler;
import com.kit.fingerprintcapture.manager.DummyDeviceManager;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.manager.MorphoDeviceManager;
import com.kit.fingerprintcapture.model.Fingerprint;
import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;

import com.kit.fingerprintcapture.model.NoFingerprintReason;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.BaseActivityArr;
import com.kit.fingerprintcapture.utils.FingerprintUtils;
import com.kit.fingerprintcapture.utils.FingerprintsManager;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class FingerprintCaptureActivity2 extends BaseActivityArr implements AdapterView.OnItemSelectedListener, DeviceDataCallback, FingerprintCaptureCallback {

    String TAG = "FingerprintCaptureActivity2";
    public static final String KEY_ENUMERATOR_REGISTRATION = "ENUMERATOR_REGISTRATION";

    public static int Width=248;
    public static int Height=448;
    private ImageView mFingerprintImage;
    private TextView mFingerprintText;
    private TextView mClickFingerprint;

    private Button mDoneBtn;

    private FingerprintCaptureHandler mfpCaptureHandler;
    private IDeviceManager mDeviceManager;

    private Fingerprint mCurrentFingerprint;

    private Animation mCurrentAnimation;

    private ThreadPoolExecutor taskExecutor;
    private boolean isDummyDevice = false;
    private boolean duplicateDetectionEnabled = true;

    private boolean mCloseClicked = false;
    private FingerprintMatchingHandler mfpMatchHandler;

    private EditText mOtherReasonTextView;
    private Boolean mHasFingerprintException;
    private NoFingerprintReason mNoFingerprintReason;
    List<NoFingerprintReason> mNoFingerprintReasonList;
    FingerprintsManager fingerprintsManager = new FingerprintsManager();
    boolean isEnumeratorRegistration = false;

//    TextView logText ;






@Override
protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_capture_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        isEnumeratorRegistration = getIntent().getBooleanExtra(KEY_ENUMERATOR_REGISTRATION, false);






        mFingerprintImage = (ImageView)findViewById(R.id.fingerprint_image);
        mFingerprintText = (TextView)findViewById(R.id.fingerprint_text);
        mClickFingerprint = (TextView)findViewById(R.id.click_fingerprint);

//        logText =  (TextView) findViewById(R.id.logText);
//        logText.setVisibility(View.VISIBLE);

        mDoneBtn = (Button)findViewById(R.id.doneBtn);

        ArrayList<Fingerprint> fingerprintList = new ArrayList<>();

        /// Matcher code that will take enumerator fingerprint and also beneficiary fingerprint and match

        Fingerprint fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_THUMB,R.id.right_thumb,R.id.right_thumb_marker,R.id.right_thumb_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_INDEX,R.id.right_index,R.id.right_index_marker,R.id.right_index_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_MIDDLE,R.id.right_middle,R.id.right_middle_marker,R.id.right_middle_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_RING,R.id.right_ring,R.id.right_ring_marker,R.id.right_ring_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_SMALL,R.id.right_small,R.id.right_small_marker,R.id.right_small_score_text);
        fingerprintList.add(fPrint);

        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_THUMB,R.id.left_thumb,R.id.left_thumb_marker,R.id.left_thumb_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_INDEX,R.id.left_index,R.id.left_index_marker,R.id.left_index_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_MIDDLE,R.id.left_middle,R.id.left_middle_marker,R.id.left_middle_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_RING,R.id.left_ring,R.id.left_ring_marker,R.id.left_ring_score_text);
        fingerprintList.add(fPrint);
        fPrint = Fingerprint.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_SMALL,R.id.left_small,R.id.left_small_marker,R.id.left_small_score_text);
        fingerprintList.add(fPrint);

        mHasFingerprintException = false;
        mNoFingerprintReasonList = NoFingerprintReason.getReasonList();
        mNoFingerprintReason = null;


        taskExecutor = new ThreadPoolExecutor(1, 3, 10, TimeUnit.MINUTES, new ArrayBlockingQueue<Runnable>(2));

        mfpCaptureHandler = new FingerprintCaptureHandler(this,fingerprintList);

        if(duplicateDetectionEnabled){
            mfpMatchHandler = new FingerprintMatchingHandler(this);
        }

        Thread t = new Thread(mfpCaptureHandler);
        t.start();

        mDoneBtn.setOnClickListener(v -> {



//
//
//            List<FingerprintData> capturedList = getCapturedFingerprints(); // You should provide this method or source
//            FingerPrintMatchChecker checker = new FingerPrintMatchChecker(this);
//
//            if (capturedList != null && !capturedList.isEmpty()) {
//                for (FingerprintData data : capturedList) {
//                    if (data != null) {
//                     //   boolean matched = checker.matchCheckAgainstEnumerator(data);
//                        Log.d(TAG, "Fingerprint ID: "
//                                + (data.getFingerprintId() != null ? data.getFingerprintId().getName() : "Unknown")
//                                + " | Matched: " + matched);
//                    } else {
//                        Log.d(TAG, "Captured fingerprint is null, skipping...");
//                    }
//                }
//            } else {
//                Log.d(TAG, "No captured fingerprints found in activity.");
//            }

//
//            List<FingerprintData> cachedList = FingerprintCache.getInstance().getFingerList();
//
//            if (cachedList != null && !cachedList.isEmpty()) {
//                for (FingerprintData data : cachedList) {
//                    logFingerprintData(data);
//                }
//            } else {
//                Log.d(TAG, "FingerprintCache is empty or null");
//            }

//

//            List<FingerprintCacheEntry> cachedList = FingerprintCache.getInstance().getFingerList();
//
//            if (cachedList != null && !cachedList.isEmpty()) {
//                for (FingerprintCacheEntry data : cachedList) {
//                    Log.d(TAG, "Cache Entry ID: " + (data.getFingerprintId() != null ? data.getFingerprintId().getName() : "Unknown"));
//                    Log.d(TAG, "Raw image size: " + (data.getRawTemplate() != null ? data.getRawTemplate(): " "));
//                }
//            } else {
//                Log.d(TAG, "FingerprintCache is empty or null");
//            }
//


            if(!isFingerprintMissing()){
                prepareReturnData();
                finish();
            }else{
                showNoFingerprintExceptionDialog();
            }
        });
        for(Fingerprint fp:fingerprintList){
            fp.getFingerprintUI().getFingerprintBtn().setOnClickListener(mfpCaptureHandler);
        }


        mCurrentFingerprint = mfpCaptureHandler.getFingerprintByID(FingerprintID.RIGHT_THUMB);

        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);




    }




    @Override
    public void onStart(){
        super.onStart();
        if(isDummyDevice)
            mDeviceManager = new DummyDeviceManager(this,this);
        else
            mDeviceManager = new MorphoDeviceManager(this,this);
    }

    @Override
    public void onPause(){
        if(BuildConfig.isDebug) {
            Log.d(TAG, "Enter onPause()");
        }
        mDeviceManager.closeDevice();

        mfpCaptureHandler.stopCapture();

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Exit onPause()");
        }

        super.onPause();
    }

    @Override
    public void onResume(){

        diableControls();
        mFingerprintText.setVisibility(View.INVISIBLE);
        mClickFingerprint.setText(R.string.device_initizing);

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

        mFingerprintText.setVisibility(View.VISIBLE);
        mClickFingerprint.setText(R.string.click_fingerprint);
        enableControls();

        super.onResume();
    }

    public void onBackPressed(){


        super.onBackPressed();
    }

    @Override
    public void onDestroy() {

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Enter onDestroy()");
        }

        mfpCaptureHandler.exitCapture();
        mDeviceManager.closeDevice();
        mDeviceManager.deInitDevice();
        taskExecutor.shutdownNow();
        while(!taskExecutor.isTerminated()){}
        mNoFingerprintReasonList=null;
        mNoFingerprintReason = null;
        super.onDestroy();

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Exit onDestroy()");
        }
    }





    public void diableControls(){
        mDoneBtn.setEnabled(false);
        for(Fingerprint fp:mfpCaptureHandler.getFingerPrintList()){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(false);
        }
    }
    public void enableControls(){
        mDoneBtn.setEnabled(true);
        for(Fingerprint fp:mfpCaptureHandler.getFingerPrintList()){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(true);
        }
    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    public void resetMarker(Fingerprint fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageDrawable(null);
    }
    public void setCaptureFinishedMarker(boolean lowScore,Fingerprint fp){

        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        if(lowScore)
            nowMarker.setImageResource(R.drawable.ico_warning);
        else
            nowMarker.setImageResource(R.drawable.ico_tick);
    }
    public void setCaptureFailedMarker(Fingerprint fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageResource(R.drawable.ico_warning);
    }
    public void setCaptureStartMarker(Fingerprint fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageResource(R.drawable.down_arrow);
    }

    public void setStartCaptureFpView(Fingerprint fp){

        ImageButton nowBtn = fp.getFingerprintUI().getFingerprintBtn();
        fp.getFingerprintUI().getFingerprintScore().setText("-");
        nowBtn.setImageResource(fp.getFingerprintID().getFpCaptureInitViewID());
        mFingerprintText.setText(R.string.capturing);

    }

    public void setFailedCaptureFpView(Fingerprint fp){

        ImageButton nowBtn = fp.getFingerprintUI().getFingerprintBtn();
        fp.getFingerprintUI().getFingerprintScore().setText("-");
        nowBtn.setImageResource(fp.getFingerprintID().getFpCaptureFailedViewID());
        mFingerprintText.setText(R.string.fingerprint_capture_failed);
    }

    public void setFinishCaptureFpView(boolean lowScore , Fingerprint fp){

        ImageButton nowBtn = fp.getFingerprintUI().getFingerprintBtn();
        String fpScore = String.valueOf(fp.getFingerprintData().getQualityScore());

        fp.getFingerprintUI().getFingerprintScore().setText(fpScore);
        if(lowScore)
            nowBtn.setImageResource(fp.getFingerprintID().getFpCapturedBadViewID());
        else
            nowBtn.setImageResource(fp.getFingerprintID().getFpCapturedGoodViewID());

        mFingerprintText.setText(R.string.fingerprint_captured);

    }

    @Override
    public void onCaptureStop(Fingerprint fp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(fp.getStatus()!=FingerprintStatus.CAPTURED){
                    fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                    fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                    resetMarker(fp);
                }

                mfpCaptureHandler.stopCapture();
            }
        });


    }
    @Override
    public void onCaptureStart(Fingerprint fp) {
        runOnUiThread(() -> {

            if(BuildConfig.isDebug) {
                Log.d("FaisalActivity", ">>>>> Entered in on onCaptureStart >>>> ");
            }

            diableControls();

            mCurrentFingerprint = fp;
            mCurrentFingerprint.setStatus(FingerprintStatus.CAPTURE_IN_PROGRESS);

            setCaptureStartMarker(fp);
            setStartCaptureFpView(fp);
            startAnimation();

            if(BuildConfig.isDebug) {
                Log.d("FaisalActivity", ">>>>> Staring autoOn >>>> ");
            }

            taskExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mDeviceManager.startCapture();
                }
            });

        });


    }
    @Override
    public void onCaptureFailed(Fingerprint fp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                    setCaptureFailedMarker(fp);
                    setFailedCaptureFpView(fp);
                    fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                    mfpCaptureHandler.captureFailed();
                }finally{
                    enableControls();
                }
            }
        });
    }

    @Override
    public void onCaptureEnd(Fingerprint fp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    fp.setStatus(FingerprintStatus.CAPTURED);
                    setCaptureFinishedMarker(fp.getFingerprintData().getQualityScore() < 60 ? true : false, fp);
                    setFinishCaptureFpView(fp.getFingerprintData().getQualityScore() < 60 ? true : false, fp);
                    fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                }finally {
                    enableControls();
                }

                mfpCaptureHandler.captureFinished();

            }
        });
    }

    public void startAnimation(){
        ImageView imView = mCurrentFingerprint.getFingerprintUI().getFingerprintMarker();
        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
        imView.startAnimation(mCurrentAnimation);
    }

//
//
//    @Override
//    public void onFingerprintData(byte[] imgData, int width, int height,int score,long result) {
//        try {
//            long ret = -1;
//
//            if (imgData != null && width > 0 && height > 0) {
//                ISOTemplate nowTmpl = TemplateUtils.createISOTemplate(imgData, width, height);
//                if(duplicateDetectionEnabled) {
//                    boolean[] matched = new boolean[1];
//
//                    /// /
//                    if (imgData != null) {
//                        FingerPrintMatchChecker checker = new FingerPrintMatchChecker();
//                        boolean matchResult = checker.matchRawAgainstEnumerator2(imgData, Width,Height);
//                        Log.d(TAG, "Fingerprint ID: "
//                              /*  + (data.getFingerprintId() != null ? data.getFingerprintId().getName() : "Unknown")*/
//                                + " | Matched: " + matchResult);
//                    } else {
//                        Log.d(TAG, "Captured fingerprint is null, skipping...");
//                    }
//                    /// /
//
//                    Log.d(TAG,"-------------------------------------\n\n\n" );
//
//                    ret = mfpMatchHandler.verifyFingerPrint(mCurrentFingerprint.getFingerprintID(), imgData, width, height, matched);
//                    if ((ret == 0) && matched[0]) {
//                        mFingerprintText.setText(R.string.duplicate_fingerprint);
//                        onCaptureError("Duplicate fingerprint");
//                        runOnUiThread(() -> Toast.makeText(FingerprintCaptureActivity2.this,"Duplicate fingerprint captured. Please recapture different finger.",Toast.LENGTH_LONG).show());
//
//                        return;
//                    }
//                }
//                byte[] wsqData = ImageProc.toWSQ(imgData, width, height);
//                mfpCaptureHandler.setFingerprintData(mCurrentFingerprint.getFingerprintID(), score, wsqData, nowTmpl.getIsoTemplate());
//                runOnUiThread(() -> {
//                    try {
//                        byte[] greyData = ImageProc.fromWSQ(mCurrentFingerprint.getFingerprintData().getFingerprintData(), width, height);
//                        mFingerprintImage.setImageBitmap(ImageProc.toGrayscale(greyData, width, height));
//                    } finally {
//                        onCaptureEnd(mCurrentFingerprint);
//                    }
//                });
//            }
//        }catch(Exception exc){
//            Log.d(TAG,exc.getMessage());
//        }
//    }
//





/*
    public void onFingerprintData2(byte[] imgData, int width, int height,int score,long result) {

        try {
            if (imgData != null && width > 0 && height > 0) {
                FingerprintData candidateFingerprintData = FingerprintUtils.imageToFingerprintDataModel(imgData,score,width,height,mCurrentFingerprint.getFingerprintID());
                ISOTemplate candidateTemplate = new ISOTemplate(candidateFingerprintData.getIsoTemplate(), candidateFingerprintData.getIsoTemplate().length);
                fingerprintsManager.removeTakenFingerFromIsoTemplate(candidateFingerprintData.getFingerprintId());
                fingerprintsManager.removeTakenFingerData(candidateFingerprintData.getFingerprintId());
                List<MatchResult> matchListForTakenFingers = new ArrayList<>();
                List<MatchResult> matchListForEnumeratorFingers = new ArrayList<>();


                mfpMatchHandler.verifyFingerPrint(candidateFingerprintData.getFingerprintId().getID(),candidateTemplate, new ArrayList<>(fingerprintsManager.getTakenFingersISOTemplates().values()), matchListForTakenFingers, TemplateFormat.ISO19794_2_2005, TemplateFormat.ISO19794_2_2005);
                mfpMatchHandler.verifyFingerPrint(mCurrentFingerprint.getFingerprintID().getID(), candidateTemplate, new ArrayList<>(fingerprintsManager.getEnumeratorISOTemplates().values()), matchListForEnumeratorFingers, TemplateFormat.ISO19794_2_2005, TemplateFormat.ISO19794_2_2005);

//
//
//                if (!matchListForTakenFingers.isEmpty()) {
//                    Log.d("FingerprintMatchTAG", "Results from reference templates:");
//                    for (MatchResult result1 : matchListForTakenFingers) {
//                        Log.d("FingerprintMatchTAG", "Matched Template ID: " + FingerprintID.getNameById(result1.getId()) +
//                                ", Match Score: " + result1.getMatchScore());
//                    }
//                } else {
//                    Log.d("FingerprintMatchTAG", "No matches found in reference templates.");
//                }
//
//                Log.d("FingerprintMatchTAG", "\n\nEnumerator template entities to verify against \n\n");
//
//                if (!matchListForEnumeratorFingers.isEmpty()) {
//                    Log.d("FingerprintMatchTAG", "Results from enumerator templates:");
//                    for (MatchResult result1 : matchListForEnumeratorFingers) {
//                        Log.d("FingerprintMatchTAG", "Matched Template ID: " +  FingerprintID.getNameById(result1.getId()) +
//                                ", Match Score: " + result1.getMatchScore());
//                    }
//                } else {
//                    Log.d("FingerprintMatchTAG", "No matches found in enumerator templates.");
//                }
//

                if (!matchListForTakenFingers.isEmpty() ) {

                    mFingerprintText.setText(R.string.duplicate_fingerprint);
                    onCaptureError("Duplicate fingerprint");
                    runOnUiThread(() -> CustomToastHandler.showErrorToast(FingerprintCaptureActivity2.this, "Duplicate fingerprint captured. Please recapture different finger."));

                    return;
                }
                if ( !matchListForEnumeratorFingers.isEmpty()) {

                    mFingerprintText.setText(R.string.duplicate_fingerprint);
                    onCaptureError("Duplicate fingerprint");
                    runOnUiThread(() -> CustomToastHandler.showErrorToast(FingerprintCaptureActivity2.this, "Fingerprint matched with the enumerator. Please recapture different finger."));

                    return;
                }




                fingerprintsManager.appendTakenFingerIsoTemplete(mCurrentFingerprint.getFingerprintID(),candidateTemplate);
                fingerprintsManager.appendTakenFingerData(candidateFingerprintData);


                mfpCaptureHandler.setFingerprintData(mCurrentFingerprint.getFingerprintID(), candidateFingerprintData.getQualityScore(), candidateFingerprintData.getFingerprintData(),candidateTemplate);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap bmp = ImageProc.toGrayscale(imgData,width,height);
                            mFingerprintImage.setImageBitmap(bmp);
                        } finally {
                            onCaptureEnd(mCurrentFingerprint);
                        }
                    }
                });
            }else{
                mFingerprintImage.setImageBitmap(ImageProc.createEmptyBitmap(width,height));
            }
        }catch(Exception exc){
            ///Log.d(TAG,exc.getMessage());
            exc.printStackTrace();
        }
    }
*/


@Override
public void onFingerprintData(byte[] imgData, int width, int height,int score,long result) {

        try {
            if (imgData != null && width > 0 && height > 0) {
                FingerprintData candidateFingerprintData = FingerprintUtils.imageToFingerprintDataModel(imgData,score,width, height,mCurrentFingerprint.getFingerprintID());
                FingerprintTemplate candidateTemplate =new FingerprintTemplate();
                candidateTemplate.dpi(500).create(imgData, width, height);
                fingerprintsManager.removeTakenFingerFromFingerprintTemplate(candidateFingerprintData.getFingerprintId());
                fingerprintsManager.removeTakenFingerData(candidateFingerprintData.getFingerprintId());
                List<MatchResult> matchListForTakenFingers = new ArrayList<>();
                List<MatchResult> matchListForEnumeratorFingers = new ArrayList<>();


// Assuming takenResults is a list or collection of FingerprintData objects
                StringBuilder logStringBuilder = new StringBuilder();
                for (FingerprintData fingerprintData : fingerprintsManager.getTakenFingers()) {
                    logStringBuilder.append("ID: ").append(fingerprintData.getFingerprintId().getID());
//                            .append("\nFingerprint Data Size: ").append(fingerprintData.getFingerprintData().length)
//                            .append("\nQuality Score: ").append(fingerprintData.getQualityScore())
//                            .append("\nISO Template Size: ").append(fingerprintData.getIsoTemplate().length)
//                            .append("\n\n");  // Adds space between each FingerprintData log
                }

// Set the log text
//                logText.setText(logStringBuilder.toString());




                mfpMatchHandler.verifyFingerPrint2(candidateFingerprintData.getFingerprintId().getID(),candidateTemplate, new ArrayList<>(fingerprintsManager.getTakenFingersTemplates().values()), matchListForTakenFingers);
//                mfpMatchHandler.verifyFingerPrint2(mCurrentFingerprint.getFingerprintID().getID(), candidateFingerprintData, new ArrayList<>(fingerprintsManager.getEnumeratorFingers()), matchListForEnumeratorFingers);

// Show detailed toast for taken fingers match results
                if (!matchListForTakenFingers.isEmpty()) {
                    StringBuilder takenResults = new StringBuilder();
                    takenResults.append("Taken Fingers Match Results:\n");
                    for (MatchResult result1 : matchListForTakenFingers) {
                        takenResults.append("ID: ").append(result1.getId())
                                .append(", Score: ").append(result1.getMatchScore())
                                .append(" - ").append(result1.getMatchScore() > 40 ? "MATCH" : "NO MATCH")
                                .append("\n");
                    }
//                    logText.setText(takenResults.toString());

                    // Toast.makeText(this, takenResults.toString(), Toast.LENGTH_LONG).show();
                } else {
//                    logText.setText( "No taken fingers matched");

                    // Toast.makeText(this, "No taken fingers matched", Toast.LENGTH_SHORT).show();
                }

// Show detailed toast for enumerator fingers match results
//                if (!matchListForEnumeratorFingers.isEmpty()) {
//                    StringBuilder enumResults = new StringBuilder();
//                    enumResults.append("Enumerator Fingers Match Results:\n");
//                    for (MatchResult result1 : matchListForEnumeratorFingers) {
//                        enumResults.append("ID: ").append(result1.getId())
//                                .append(", Score: ").append(result1.getMatchScore())
//                                .append(" - ").append(result1.getMatchScore() > 40 ? "MATCH" : "NO MATCH")
//                                .append("\n");
//                    }
//                   // logText.setText(enumResults.toString());
//                   // Toast.makeText(this, enumResults.toString(), Toast.LENGTH_LONG).show();
//                } else {
//                   // logText.setText("No enumerator fingers matched");
//                   // Toast.makeText(this, "No enumerator fingers matched", Toast.LENGTH_SHORT).show();
//                }



//
//
//                if (!matchListForTakenFingers.isEmpty()) {
//                    Log.d("FingerprintMatchTAG", "Results from reference templates:");
//                    for (MatchResult result1 : matchListForTakenFingers) {
//                        Log.d("FingerprintMatchTAG", "Matched Template ID: " + FingerprintID.getNameById(result1.getId()) +
//                                ", Match Score: " + result1.getMatchScore());
//                    }
//                } else {
//                    Log.d("FingerprintMatchTAG", "No matches found in reference templates.");
//                }
//
//                Log.d("FingerprintMatchTAG", "\n\nEnumerator template entities to verify against \n\n");
//
//                if (!matchListForEnumeratorFingers.isEmpty()) {
//                    Log.d("FingerprintMatchTAG", "Results from enumerator templates:");
//                    for (MatchResult result1 : matchListForEnumeratorFingers) {
//                        Log.d("FingerprintMatchTAG", "Matched Template ID: " +  FingerprintID.getNameById(result1.getId()) +
//                                ", Match Score: " + result1.getMatchScore());
//                    }
//                } else {
//                    Log.d("FingerprintMatchTAG", "No matches found in enumerator templates.");
//                }
//

                if (!matchListForTakenFingers.isEmpty() ) {

                    mFingerprintText.setText(R.string.duplicate_fingerprint);
                    onCaptureError("Duplicate fingerprint");
                    runOnUiThread(() -> CustomToastHandler.showErrorToast(FingerprintCaptureActivity2.this, "Duplicate fingerprint captured. Please recapture different finger."));

                    return;
                }
                if ( !matchListForEnumeratorFingers.isEmpty()) {

                    mFingerprintText.setText(R.string.duplicate_fingerprint);
                    onCaptureError("Duplicate fingerprint");
                    runOnUiThread(() -> CustomToastHandler.showErrorToast(FingerprintCaptureActivity2.this, "Fingerprint matched with the enumerator. Please recapture different finger."));

                    return;
                }

                fingerprintsManager.appendTakenFingerFingerTemplete(mCurrentFingerprint.getFingerprintID(),candidateTemplate);
                fingerprintsManager.appendTakenFingerData(candidateFingerprintData);
                mfpCaptureHandler.setFingerprintData(mCurrentFingerprint.getFingerprintID(), candidateFingerprintData.getQualityScore(), candidateFingerprintData.getFingerprintData(),candidateFingerprintData.getIsoTemplate());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap bmp = ImageProc.toGrayscale(imgData,width,height);
                            mFingerprintImage.setImageBitmap(bmp);
                        } finally {
                            onCaptureEnd(mCurrentFingerprint);
                        }
                    }
                });
            }else{
              //  mFingerprintImage.setImageBitmap(ImageProc.createEmptyBitmap(width,height));
            }
        }catch(Exception exc){
            ///Log.d(TAG,exc.getMessage());
            exc.printStackTrace();
        }
    }



    @Override
    public void onFingerprintPreview(Bitmap img, int width, int height) {
        runOnUiThread(() -> {
            try {
                mFingerprintImage.setImageBitmap(img);
            }catch (Exception exc){
                Log.e(TAG,"Preview show error");
            }
        });
    }

    @Override
    public void onCaptureCmd(String cmd) {
        if(cmd!=null && cmd.length()>0){
            mFingerprintText.setText(cmd);
        }
    }

    @Override
    public void onCaptureError(String Error) {
        runOnUiThread(() -> {
            try {
                int width=248;
                int height=448;
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                mFingerprintImage.setImageBitmap(bitmap);
            }finally {
                onCaptureFailed(mCurrentFingerprint);
            }
        });
    }

    public boolean isFingerprintMissing(){
        for (Fingerprint fp : mfpCaptureHandler.getFingerPrintList()) {
            if(fp.getFingerprintData().getFingerprintData()==null) {
                return true;
            }
        }
        return false;
    }

    public void showNoFingerprintExceptionDialog() {
        mHasFingerprintException = true;

        // Inflate the dialog view
        View dialogView = LayoutInflater.from(this).inflate(R.layout.no_finger_drop_down, null);

        // Initialize views
        MaterialAutoCompleteTextView reasonSpinner = dialogView.findViewById(R.id.spinner);
        Button okButton = dialogView.findViewById(R.id.okBtn);
        Button closeButton = dialogView.findViewById(R.id.closeBtn);
        mOtherReasonTextView = dialogView.findViewById(R.id.otherReasonText);
        // EditText otherReasonEditText = mOtherReasonTextView; // Optional alias if needed

        // Configure EditText
        configureEditText(mOtherReasonTextView);

        // Setup spinner
        setupSpinner(reasonSpinner, mOtherReasonTextView);

        // Create and configure dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.noFingerprintExceptionDlgTitle)
                .setIcon(R.drawable.logo_splash)
                .setView(dialogView)
                .setCancelable(false)
                .setOnDismissListener(this::handleDialogDismiss)
                .create();

        // Set button click listeners
        okButton.setOnClickListener(v -> handleOkClick(dialog, mOtherReasonTextView));
        closeButton.setOnClickListener(v -> handleCloseClick(dialog));

        dialog.show();
    }


    private void configureEditText(EditText editText) {
        editText.setTextColor(Color.BLACK);
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        editText.setEnabled(false);
        editText.setBackgroundResource(R.drawable.border_disabled);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });
    }

    private void setupSpinner(MaterialAutoCompleteTextView spinner, EditText otherReasonEditText) {
        mNoFingerprintReasonList = NoFingerprintReason.getReasonList();
        mNoFingerprintReason = NoFingerprintReason.getNoFingerPrintReasonByID(1);

        // Create adapter with Material Design item layout
        ArrayAdapter<NoFingerprintReason> adapter = new ArrayAdapter<>(
                this,
                R.layout.md_spinner_item,
                mNoFingerprintReasonList
        );
        adapter.setDropDownViewResource(R.layout.md_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set initial selection
        spinner.setText(mNoFingerprintReason.toString(), false);

        spinner.setOnItemClickListener((parent, view, position, id) -> {
            mNoFingerprintReason = mNoFingerprintReasonList.get(position);
            boolean isOtherReason = mNoFingerprintReason == NoFingerprintReason.Other;

            otherReasonEditText.setEnabled(isOtherReason);
            otherReasonEditText.setBackgroundResource(isOtherReason ?
                    R.drawable.border : R.drawable.border_disabled);

            if (!isOtherReason) {
                otherReasonEditText.setText("");
                otherReasonEditText.setHint("");
                otherReasonEditText.setTextColor(Color.BLACK);
            }
        });
    }

    private void handleOkClick(AlertDialog dialog, EditText otherReasonEditText) {
        if (mNoFingerprintReason == NoFingerprintReason.Other &&
                TextUtils.isEmpty(otherReasonEditText.getText().toString().trim())) {

            otherReasonEditText.setBackgroundResource(R.drawable.border_error);
            otherReasonEditText.setHint("Please write a reason");
            otherReasonEditText.setTextColor(Color.RED);
            return;
        }

        prepareReturnData();
        dialog.dismiss();
        finish();
    }

    private void handleCloseClick(AlertDialog dialog) {
        mCloseClicked = true;
        mHasFingerprintException = false;
        mNoFingerprintReason = null;
        mOtherReasonTextView = null;
        mNoFingerprintReasonList = null;
        dialog.dismiss();
    }

    private void handleDialogDismiss(DialogInterface dialog) {
        if (!mCloseClicked) {
            finish();
        } else {
            mCloseClicked = false;
        }
    }


    public void prepareReturnData() {
        createFingerprintCache(); // Save full fingerprint data in cache

        Intent data = new Intent();
        try {
            data.putExtra("noFingerprint", mHasFingerprintException);

            if (BuildConfig.isDebug) {
                Log.d(TAG, "noFIngerprint : " + mHasFingerprintException);
            }

            if (mHasFingerprintException) {
                data.putExtra("noFingerprintReasonID", mNoFingerprintReason.getNoFingerprintReasonID());

                if (BuildConfig.isDebug) {
                    Log.d(TAG, "noFingerprintReasonID : " + mNoFingerprintReason.getNoFingerprintReasonID());
                }

//                if (mNoFingerprintReason == NoFingerprintReason.Other) {
//                    if (mOtherReasonTextView != null) {
//                        data.putExtra("noFingerprintReasonText", mOtherReasonTextView.getText().toString());
//                        if (BuildConfig.isDebug) {
//                            Log.d(TAG, "noFingerprintReasonText : " + mOtherReasonTextView.getText().toString());
//                        }
//                    } else {
//                        data.putExtra("noFingerprintReasonText", "");
//                    }
//                } else {
//                    data.putExtra("noFingerprintReasonText", "");
//                }

                if (mNoFingerprintReason != null && mNoFingerprintReason.getNoFingerprintReasonID() == NoFingerprintReason.Other.getNoFingerprintReasonID()) {
                    if (mOtherReasonTextView != null) {
                        data.putExtra("noFingerprintReasonText", mOtherReasonTextView.getText().toString());
                        Log.d(TAG, "noFingerprintReasonText : " + mOtherReasonTextView.getText().toString());
                        Log.d(TAG, "noFingerprintReasonText : " + " not null ");
                    } else {
                        Log.d(TAG, "noFingerprintReasonText : " + " null ");
                        data.putExtra("noFingerprintReasonText", "");
                        Log.d(TAG, "noFingerprintReasonText : " + " NoFingerprintReason not Other  :   " + mNoFingerprintReason.getNoFingerprintReasonText() + "   " + mNoFingerprintReason);
                    }
                }


                Log.d(TAG, "noFingerprintReasonText : " + mNoFingerprintReason.getNoFingerprintReasonID()  + "  "+ NoFingerprintReason.Other.getNoFingerprintReasonID());

            }

            // ✅ Only pass a simple status string instead of raw fingerprint data
            data.putExtra("fingerprintCaptureStatus", "successful");

            // ✅ Clear fingerprint memory
            clearFingerprintMemory();

            setResult(Activity.RESULT_OK, data);
        } catch (Throwable t) {
            setResult(Activity.RESULT_CANCELED, data);
            t.printStackTrace();
        }
    }



    private void createFingerprintCache() {
        FingerprintCache fingerprintCache = FingerprintCache.getInstance();
        List<FingerprintData> fingerDataList = new ArrayList<>();

        for (Fingerprint fp : mfpCaptureHandler.getFingerPrintList()) {
            FingerprintData data = fp.getFingerprintData();

            if (data != null && data.getFingerprintData() != null) {
                // Create a new FingerprintData object and copy fields
                FingerprintData clonedData = new FingerprintData();
                clonedData.setFingerprintId(data.getFingerprintId());
                clonedData.setFingerprintData(data.getFingerprintData() != null ? data.getFingerprintData().clone() : null);
                clonedData.setIsoTemplate(data.getIsoTemplate() != null ? data.getIsoTemplate().clone() : null);
                clonedData.setQualityScore(data.getQualityScore());

                fingerDataList.add(clonedData);


                String fingerprintID = (fp.getFingerprintID() != null) ? fp.getFingerprintID().getName() : "Unknown";
                int fingerprintDataSize = (data.getFingerprintData() != null) ? data.getFingerprintData().length : 0;
                long qualityScore = data.getQualityScore();
                int isoTemplateSize = (data.getIsoTemplate() != null) ? data.getIsoTemplate().length : 0;

                Log.d("FingerprintCache", "----------------------------");
                Log.d("FingerprintCache", "Fingerprint ID: " + fingerprintID);
                Log.d("FingerprintCache", "Data size (bytes): " + fingerprintDataSize);
                Log.d("FingerprintCache", "Quality Score: " + qualityScore);
                Log.d("FingerprintCache", "ISO Template size (bytes): " + isoTemplateSize);
                Log.d("FingerprintCache", "----------------------------");


            } else {
                Log.w("FingerprintCache", "Skipped fingerprint with null data for ID: " +
                        (fp.getFingerprintID() != null ? fp.getFingerprintID().getName() : "Unknown"));
            }
        }

        fingerprintCache.setFingerList(fingerDataList);
    }



    private void clearFingerprintMemory() {
        if (mfpCaptureHandler != null && mfpCaptureHandler.getFingerPrintList() != null) {
            for (Fingerprint fp : mfpCaptureHandler.getFingerPrintList()) {
                if (fp.getFingerprintData() != null) {
                    fp.getFingerprintData().setFingerprintData(null);
                    fp.getFingerprintData().setIsoTemplate(null);
                }
            }
            mfpCaptureHandler.getFingerPrintList().clear();
        }
        Log.d("FingerprintCache", "Fingerprint cache cleared------------");
        // Optional: if mfpCaptureHandler is no longer needed
        //   mfpCaptureHandler = null;

        // Nullify UI elements if not needed anymore
        //  mOtherReasonTextView = null;

        // Suggest garbage collection (optional but may help on low-memory devices)
        System.gc();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mNoFingerprintReason = NoFingerprintReason.getNoFingerPrintReasonByID(position+1);
        if(position==5){
            mOtherReasonTextView.setEnabled(true);
            mOtherReasonTextView.setBackgroundResource(R.drawable.border);
            mOtherReasonTextView.setHint("");
        }else {
            mOtherReasonTextView.setEnabled(false);
            mOtherReasonTextView.setBackgroundResource(R.drawable.border_disabled);
            mOtherReasonTextView.setHint("");
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}