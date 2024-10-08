package com.kit.fingerprintcapture;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.Bundle;

import android.text.InputFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dermalog.afis.fingercode3.FC3Exception;
import com.dermalog.afis.fingercode3.Matcher;
import com.dermalog.afis.fingercode3.TemplateFormat;
import com.dermalog.common.exception.ErrorCodes;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.common.CustomToastHandler;
import com.kit.fingerprintcapture.adapters.FingerprintExceptionListAdapter;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.callback.FingerprintCaptureCallback;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.handlers.FingerprintMatchingHandler;
import com.kit.fingerprintcapture.manager.DummyDeviceManager;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.manager.DermalogDeviceManager;
import com.kit.fingerprintcapture.model.Fingerprint;

import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;

import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.model.NoFingerprintReason;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.FileUtils;
import com.kit.fingerprintcapture.utils.ImageProc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FingerprintCaptureActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DeviceDataCallback, FingerprintCaptureCallback {

    String TAG = "FingerprintCaptureActivity";

    private ImageView mFingerprintImage;
    private TextView mFingerprintText;
    private TextView mClickFingerprint;

    private Button mDoneBtn;

    private FingerprintCaptureHandler mfpCaptureHandler;
    private IDeviceManager mDeviceManager;

    private Fingerprint mCurrentFingerprint;

    private Animation mCurrentAnimation;


    private ExecutorService mFPCaptureService;/// = Executors.newSingleThreadExecutor();


    private ExecutorService mFPStartCaptureService;/// = Executors.newSingleThreadExecutor();

    private boolean isDummyDevice = false;
    private boolean duplicateDetectionEnabled = true;
    private boolean mCloseClicked = false;
    private FingerprintMatchingHandler mfpMatchHandler;

    private EditText mOtherReasonTextView;
    private Boolean mHasFingerprintException;
    private NoFingerprintReason mNoFingerprintReason;
    private List<NoFingerprintReason> mNoFingerprintReasonList;

    private Map<Integer, ISOTemplate> mReferenceTemplateList;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fingerprint_capture_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mFingerprintImage = (ImageView)findViewById(R.id.fingerprint_image);
        mFingerprintText = (TextView)findViewById(R.id.fingerprint_text);
        mClickFingerprint = (TextView)findViewById(R.id.click_fingerprint);

        mDoneBtn = (Button)findViewById(R.id.doneBtn);

        ArrayList<Fingerprint> fingerprintList = new ArrayList<>();

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


        mFPCaptureService = Executors.newSingleThreadExecutor();
        mfpCaptureHandler = new FingerprintCaptureHandler(this,fingerprintList);
        mFPCaptureService.submit(mfpCaptureHandler);



        mfpMatchHandler = new FingerprintMatchingHandler(this);


        mReferenceTemplateList = new HashMap<>();

        mDoneBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (!isFingerprintMissing()) {
                        prepareReturnData();
                        finish();
                    } else {
                        showNoFingerprintExceptionDialog();
                    }
                }
        });
        for(Fingerprint fp:fingerprintList){
            fp.getFingerprintUI().getFingerprintBtn().setOnClickListener(mfpCaptureHandler);
        }


        mCurrentFingerprint = mfpCaptureHandler.getFingerprintByID(FingerprintID.RIGHT_THUMB);

        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);

        if(isDummyDevice)
            mDeviceManager = new DummyDeviceManager(this,this);
        else
            mDeviceManager = new DermalogDeviceManager(this,this);

    }
    @Override
    public void onStart(){
        super.onStart();
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
        super.onResume();

        diableControls();

        mFingerprintText.setVisibility(View.INVISIBLE);
        mClickFingerprint.setText(R.string.device_initizing);

        try {
            long result = mDeviceManager.initDevice();

            if(BuildConfig.isDebug){
                Log.d(TAG, "initDevice() returned : " + result);
            }

            if(result!= ErrorCodes.FPC_SUCCESS){
                showNoAccessToDevice();
                return;
            }else{
                result = mDeviceManager.openDevice();
                if(result!=ErrorCodes.FPC_SUCCESS) {
                    showFingerprintDeviceNotInitialized(result);
                }else{
                    mFingerprintText.setVisibility(View.VISIBLE);
                    mClickFingerprint.setText(R.string.click_fingerprint);

                    if(!isDummyDevice) {
                        try {
                            Matcher nowMatcher = new Matcher();
                            nowMatcher.setRotationToleranceInDegree(180);
                            mfpMatchHandler.setMatcher(nowMatcher);
                        } catch (FC3Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "FingerCode3: NO LICENSE", Toast.LENGTH_LONG).show();
                            mfpMatchHandler.setMatcher(null);
                        }
                    }else{
                        mfpMatchHandler.setMatcher(null);
                    }
                }
            }
        }catch(Throwable t){
            t.printStackTrace();
        }finally {
            if (!isDummyDevice){
                if (mfpMatchHandler.getMatcher() == null){
                    showFingerprintDeviceNotInitialized(-1);
                }
            }
        }

        enableControls();
    }

    @Override
    public void onDestroy() {

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Enter onDestroy()");
        }

        mfpCaptureHandler.exitCapture();
        mDeviceManager.closeDevice();
        mDeviceManager.deInitDevice();

        if(mfpMatchHandler!=null){
            mfpMatchHandler.setMatcher(null);
        }

        if(mFPStartCaptureService!=null){
            try {
                mFPStartCaptureService.shutdownNow();
                while(mFPStartCaptureService.isTerminated()){
                    Thread.sleep(50);
                }
            }catch(Throwable t){
                Log.e(TAG,"Error while stopping device capture handler");
            }
        }
        if(mFPCaptureService!=null){
            try {
                mFPCaptureService.shutdownNow();
                while(mFPCaptureService.isTerminated()){
                    Thread.sleep(50);
                }
            }catch(Throwable t){
                Log.e(TAG,"Error while stopping capture handler");
            }
        }
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
                synchronized (FingerprintCaptureActivity.this) {
                    if (fp.getStatus() != FingerprintStatus.CAPTURED) {
                        fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                        fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                        resetMarker(fp);
                    }

                    mfpCaptureHandler.stopCapture();
                }
            }
        });
    }
    @Override
    public void onCaptureStart(Fingerprint fp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(BuildConfig.isDebug) {
                    Log.d("FaisalActivity", ">>>>> Entered in on onCaptureStart >>>> ");
                }
                synchronized (FingerprintCaptureActivity.this) {
                    diableControls();

                    mCurrentFingerprint = fp;
                    mCurrentFingerprint.setStatus(FingerprintStatus.CAPTURE_IN_PROGRESS);

                    setCaptureStartMarker(fp);
                    setStartCaptureFpView(fp);
                    startAnimation();
                    Callable<Void> nowCallable = new Callable<Void>() {
                        @Override
                        public Void call() throws Exception {
                            mDeviceManager.startCapture();
                            return Void.TYPE.newInstance();
                        }
                    };

                    mFPStartCaptureService = Executors.newSingleThreadExecutor();
                    mFPStartCaptureService.submit(nowCallable);

                }

            }
        });

    }

        @Override
        public void onCaptureFailed(Fingerprint fp) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (FingerprintCaptureActivity.this) {
                        try {
                            fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                            setCaptureFailedMarker(fp);
                            setFailedCaptureFpView(fp);
                            fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                            mfpCaptureHandler.captureFailed();
                        } finally {
                            enableControls();
                        }
                    }
                }
            });
        }

    @Override
    public void onCaptureEnd(Fingerprint fp) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (FingerprintCaptureActivity.this) {
                    try {
                        fp.setStatus(FingerprintStatus.CAPTURED);
                        setCaptureFinishedMarker(fp.getFingerprintData().getQualityScore() < 50 ? true : false, fp);
                        setFinishCaptureFpView(fp.getFingerprintData().getQualityScore() < 50 ? true : false, fp);
                        fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                    } finally {
                        enableControls();
                    }
                    mfpCaptureHandler.captureFinished();
                }
            }
        });
    }

    public void startAnimation(){
        ImageView imView = mCurrentFingerprint.getFingerprintUI().getFingerprintMarker();
        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
        imView.startAnimation(mCurrentAnimation);
    }

    @Override
    public void onFingerprintData(byte[] imgData, int width, int height,int score,long result) {
        try {
            long ret = -1;

            if (imgData != null && width > 0 && height > 0) {

                long imScore = ImageProc.computeScore(imgData,width,height);
                score = ImageProc.mapNFIQScore((int)imScore);

                if(BuildConfig.isDebug) {
                    Log.d(TAG, "NFIQ Score is : " + imScore);
                }

                byte[] wsqData = ImageProc.toWSQ(imgData, width, height);
                FileUtils.saveByteArrayToFile(wsqData,"fingerImage",FingerprintCaptureActivity.this);
//                if(duplicateDetectionEnabled && !isDummyDevice) {
                  if(true) {

                   ISOTemplate template = mfpMatchHandler.createISOTemplate(imgData,width,height);

                    if(BuildConfig.isDebug) {
                        if(template==null) {
                            Log.d(TAG, "Received null template");
                        }else{
                            Log.d(TAG, "Received a template with size = "+template.getIsoTemplateSize());
                        }
                    }

                    if (mReferenceTemplateList.containsKey(mCurrentFingerprint.getFingerprintID().getID())) {
                        mReferenceTemplateList.remove(mCurrentFingerprint.getFingerprintID().getID());
                    }

                    if (mReferenceTemplateList.size() > 0) {

                        if(true) {

                            List<MatchResult> matchList = new ArrayList<>();

                            mfpMatchHandler.verifyFingerPrint(mCurrentFingerprint.getFingerprintID().getID(),
                                    template, mReferenceTemplateList.values().stream().collect(Collectors.toList()), matchList, TemplateFormat.ISO19794_2_2005,TemplateFormat.ISO19794_2_2005);

                            if (matchList.size() > 0) {

                                mFingerprintText.setText(R.string.duplicate_fingerprint);

                                onCaptureError("Duplicate fingerprint");

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        CustomToastHandler.showErrorToast(FingerprintCaptureActivity.this, "Duplicate fingerprint captured. Please recapture different finger.");
                                    }
                                });

                                return;
                            }
                        }
                    }

                    mReferenceTemplateList.put(mCurrentFingerprint.getFingerprintID().getID(),template);
                }

                mfpCaptureHandler.setFingerprintData(mCurrentFingerprint.getFingerprintID(), score, wsqData);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Bitmap bmp = ImageProc.toGrayscale(imgData,width,height);
                            mFingerprintImage.setImageBitmap(bmp);
                            //                            FileUtils.saveBitmapToFile(bmp,"fingerImageBitmap",FingerprintCaptureActivity.this);
//                            mFingerprintImage.setImageBitmap(BitmapUtil.fromBitmapInfoHeaderData(imgData, Bitmap.Config.ARGB_8888));
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

    @Override
    public void onFingerprintPreview(Bitmap img, int width, int height) {
        Log.d(TAG, "onFingerprintPreview() called with: img = [" + img + "], width = [" + width + "], height = [" + height + "]");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mFingerprintImage.setImageBitmap(img);
                }catch (Exception exc){
                    Log.e(TAG,"Preview show error");
                }
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
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    int width=248;
                    int height=448;
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    mFingerprintImage.setImageBitmap(bitmap);
                }finally {
                    onCaptureFailed(mCurrentFingerprint);
                }
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

    public boolean isFingerprintCaptured(){
        for (Fingerprint fp : mfpCaptureHandler.getFingerPrintList()) {
            if(fp.getFingerprintData().getFingerprintData()!=null) {
                return true;
            }
        }
        return false;
    }

    public void showNoFingerprintExceptionDialog(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setIcon(R.drawable.logo_splash);
        builder.setTitle(R.string.noFingerprintExceptionDlgTitle);

        mHasFingerprintException = true;
        ViewGroup viewGroup = findViewById(android.R.id.content);

        View mView = LayoutInflater.from(this).inflate(R.layout.no_finger_drop_down, viewGroup, false);

        Spinner reasonSpinner = (Spinner) mView.findViewById(R.id.spinner);

        reasonSpinner.setOnItemSelectedListener(this);
        Button ok = (Button) mView.findViewById(R.id.okBtn);
        Button close = (Button) mView.findViewById(R.id.closeBtn);

        mOtherReasonTextView = (EditText) mView.findViewById(R.id.otherReasonText);
        mOtherReasonTextView.setTextColor(Color.BLACK);
        mOtherReasonTextView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(100)});
        mOtherReasonTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                    inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        });


        mOtherReasonTextView.setEnabled(false);
        mOtherReasonTextView.setBackgroundResource(R.drawable.border_disabled);
        mNoFingerprintReasonList = NoFingerprintReason.getReasonList();
        mHasFingerprintException = true;
        mNoFingerprintReason = NoFingerprintReason.getNoFingerPrintReasonByID(1);
        FingerprintExceptionListAdapter mAdapter = new FingerprintExceptionListAdapter(getApplicationContext(), mNoFingerprintReasonList);
        reasonSpinner.setAdapter(mAdapter);

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!mCloseClicked)
                    FingerprintCaptureActivity.this.finish();
                else
                    mCloseClicked = false;
            }
        });

        builder.setCancelable(false);
        builder.setView(mView);
        AlertDialog dialog = builder.create();
        dialog.show();
        ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mNoFingerprintReason == NoFingerprintReason.Other &&
                        (mOtherReasonTextView.getText() == null || mOtherReasonTextView.getText().toString().trim().length() <= 0)) {    //Could not find logic so kept it true
                    mOtherReasonTextView.setBackgroundResource(R.drawable.border_error);
                    mOtherReasonTextView.setHint("Please write a reason");
                    mOtherReasonTextView.setTextColor(Color.RED);
                } else {
                    prepareReturnData();
                    dialog.dismiss();
                    finish();
                }
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCloseClicked = true;
                mHasFingerprintException = false;
                mNoFingerprintReason = null;
                mOtherReasonTextView = null;
                mNoFingerprintReasonList = null;
                dialog.dismiss();
            }
        });
    }

    public void prepareReturnData() {
        Intent data = new Intent();
        try{

            data.putExtra("noFingerprint",mHasFingerprintException);

            if(BuildConfig.isDebug) {
                Log.d(TAG, "noFIngerprint : " + mHasFingerprintException);
            }

            if (mHasFingerprintException) {
                data.putExtra("noFingerprintReasonID", mNoFingerprintReason.getNoFingerprintReasonID());

                if (BuildConfig.isDebug) {
                    Log.d(TAG, "noFingerprintReasonID : " + mNoFingerprintReason.getNoFingerprintReasonID());
                }

                if (mNoFingerprintReason == NoFingerprintReason.Other) {
                    if (mOtherReasonTextView != null) {
                        data.putExtra("noFingerprintReasonText", mOtherReasonTextView.getText().toString());
                        if (BuildConfig.isDebug) {
                            Log.d(TAG, "noFingerprintReasonText : " + mOtherReasonTextView.getText().toString());
                        }
                    } else {
                        data.putExtra("noFingerprintReasonText", "");
                    }
                } else {
                    data.putExtra("noFingerprintReasonText", "");
                }
            }

            for (Fingerprint fp : mfpCaptureHandler.getFingerPrintList()) {
                if(fp.getFingerprintData().getFingerprintData()!=null) {

                    if(BuildConfig.isDebug) {
                        Log.d("TAG", "Fingerprint data size : " + (fp.getFingerprintData().getFingerprintData().length));
                        Log.d(TAG, "Setting data for : " + fp.getFingerprintID().getName());
                    }

                    data.putExtra(fp.getFingerprintID().getName(), fp.getFingerprintData());
                }
            }
            setResult(Activity.RESULT_OK, data);
        }catch(Throwable t){
            setResult(Activity.RESULT_CANCELED,data);
            t.printStackTrace();
        }
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



    public void showToast(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(FingerprintCaptureActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showNoAccessToDevice(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(FingerprintCaptureActivity.this).create();
                alertDialog.setCancelable(false);
                alertDialog.setTitle(R.string.app_name);
                alertDialog.setMessage(getString(R.string.noAccessToDevice));
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        FingerprintCaptureActivity.this.finish();
                    }
                });
                alertDialog.show();
            }
        });
    }

    private void showFingerprintDeviceNotInitialized(long result){
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Fingerprint device open failed with error : " + result);
        dlgAlert.setTitle("Fingerprint Registration");
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