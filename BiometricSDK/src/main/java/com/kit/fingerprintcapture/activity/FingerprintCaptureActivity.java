package com.kit.fingerprintcapture.activity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;

import android.graphics.Color;
import android.os.Bundle;

import android.os.Parcelable;
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

import com.dermalog.common.exception.ErrorCodes;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.adapters.FingerprintExceptionListAdapter;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.callback.FingerprintCaptureCallback;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.handlers.MorphoMatchingHandler;
import com.kit.fingerprintcapture.manager.DummyDeviceManager;
import com.kit.fingerprintcapture.manager.IDeviceManager;
import com.kit.fingerprintcapture.manager.MorphoDeviceManager;
import com.kit.fingerprintcapture.model.FingerprintCaptureItem;

import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;

import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.model.NoFingerprintReason;
import com.kit.fingerprintcapture.utils.FingerprintUtils;
import com.kit.fingerprintcapture.utils.ImageProc;

import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FingerprintCaptureActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, DeviceDataCallback, FingerprintCaptureCallback {

    String TAG = "FingerprintCaptureActivity";

    private ImageView mFingerprintImage;


    private TextView mFingerprintText;
    private TextView mClickFingerprint;

    private Button mDoneBtn;

    private FingerprintCaptureHandler mfpCaptureHandler;
    private IDeviceManager mDeviceManager;

    private FingerprintCaptureItem mCurrentFingerprintCaptureItem;
    private Animation mCurrentAnimation;
    private ExecutorService mFPCaptureService;/// = Executors.newSingleThreadExecutor();

    private ExecutorService mFPStartCaptureService;/// = Executors.newSingleThreadExecutor();

    private boolean isDummyDevice = false;
    private boolean duplicateDetectionEnabled = true;
    private boolean mCloseClicked = false;
    private MorphoMatchingHandler mfpMatchHandler;
    private EditText mOtherReasonTextView;
    private Boolean mHasFingerprintException;
    private NoFingerprintReason mNoFingerprintReason;
    private List<NoFingerprintReason> mNoFingerprintReasonList;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.fingerprint_capture_layout);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        bindUi();

        mHasFingerprintException = false;
        mNoFingerprintReasonList = NoFingerprintReason.getReasonList();
        mNoFingerprintReason = null;

        mFPCaptureService = Executors.newSingleThreadExecutor();

        mfpMatchHandler = new MorphoMatchingHandler(this);
    }

    private void bindUi(){

        mFingerprintImage = findViewById(R.id.fingerprint_image);
        mFingerprintText = findViewById(R.id.fingerprint_text);
        mClickFingerprint = findViewById(R.id.click_fingerprint);

        mDoneBtn = findViewById(R.id.doneBtn);

        mDoneBtn.setOnClickListener(v -> onDoneClicked());

        mCurrentFingerprintCaptureItem = mfpCaptureHandler.getFingerprintByID(FingerprintID.RIGHT_THUMB);

        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);

        bindFingers();


    }
    private void bindFingers(){

        ArrayList<FingerprintCaptureItem> fingerprintCaptureItemList = new ArrayList<>();

        FingerprintCaptureItem fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_THUMB,R.id.right_thumb,R.id.right_thumb_marker,R.id.right_thumb_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_INDEX,R.id.right_index,R.id.right_index_marker,R.id.right_index_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_MIDDLE,R.id.right_middle,R.id.right_middle_marker,R.id.right_middle_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_RING,R.id.right_ring,R.id.right_ring_marker,R.id.right_ring_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.RIGHT_SMALL,R.id.right_small,R.id.right_small_marker,R.id.right_small_score_text);
        fingerprintCaptureItemList.add(fPrint);

        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_THUMB,R.id.left_thumb,R.id.left_thumb_marker,R.id.left_thumb_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_INDEX,R.id.left_index,R.id.left_index_marker,R.id.left_index_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_MIDDLE,R.id.left_middle,R.id.left_middle_marker,R.id.left_middle_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_RING,R.id.left_ring,R.id.left_ring_marker,R.id.left_ring_score_text);
        fingerprintCaptureItemList.add(fPrint);
        fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(),FingerprintID.LEFT_SMALL,R.id.left_small,R.id.left_small_marker,R.id.left_small_score_text);
        fingerprintCaptureItemList.add(fPrint);

        for(FingerprintCaptureItem fp: fingerprintCaptureItemList){
            fp.getFingerprintUI().getFingerprintBtn().setOnClickListener(mfpCaptureHandler);
        }

        mfpCaptureHandler = new FingerprintCaptureHandler(this, fingerprintCaptureItemList);
        mFPCaptureService.submit(mfpCaptureHandler);
    }



    private void onDoneClicked(){

        prepareReturnData();
        //      finish();
//                if (!isFingerprintMissing()) {
//                    Log.d(TAG, "onCreate() called with: if]");
//                    prepareReturnData();
//                    finish();
//                } else {
//                    Log.d(TAG, "onCreate() called with: else]");
//                    showNoFingerprintExceptionDialog();
//                }

    }

    @Override
    public void onStart(){
        super.onStart();
        if(isDummyDevice){
            mDeviceManager = new DummyDeviceManager(this,this);
        } else {
            mDeviceManager = new MorphoDeviceManager(this,this);
            Log.d(TAG, "morpho    ");
        }
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

        disableControls();

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
                if(result != ErrorCodes.FPC_SUCCESS) {
                    showFingerprintDeviceNotInitialized(result);
                }else{
                    mFingerprintText.setVisibility(View.VISIBLE);
                    mClickFingerprint.setText(R.string.click_fingerprint);

                    if(!isDummyDevice) {
//                        try {
//                            Matcher nowMatcher = new Matcher();
//                            nowMatcher.setRotationToleranceInDegree(180);
//                            mfpMatchHandler.setMatcher(nowMatcher);
//                        } catch (FC3Exception e) {
//                            e.printStackTrace();
//                            Toast.makeText(this, "FingerCode3: NO LICENSE", Toast.LENGTH_LONG).show();
//                            mfpMatchHandler.setMatcher(null);
//                        }
                    }else{
                       // mfpMatchHandler.setMatcher(null);
                    }
                }
            }
        }catch(Throwable t){
            t.printStackTrace();
        }finally {
//            if (!isDummyDevice){
//                if (mfpMatchHandler.getMatcher() == null){
//                    showFingerprintDeviceNotInitialized(-1);
//                }
//            }
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
            mfpMatchHandler = new MorphoMatchingHandler(this);
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
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public void disableControls(){
        mDoneBtn.setEnabled(false);
        for(FingerprintCaptureItem fp:mfpCaptureHandler.getFingerPrintList()){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(false);
        }
    }
    public void enableControls(){
        mDoneBtn.setEnabled(true);
        for(FingerprintCaptureItem fp:mfpCaptureHandler.getFingerPrintList()){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(true);
        }
    }


    public void resetMarker(FingerprintCaptureItem fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageDrawable(null);
    }
    public void setCaptureFinishedMarker(boolean lowScore, FingerprintCaptureItem fp){

        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        if(lowScore)
            nowMarker.setImageResource(R.drawable.ico_warning);
        else
            nowMarker.setImageResource(R.drawable.ico_tick);
    }
    public void setCaptureFailedMarker(FingerprintCaptureItem fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageResource(R.drawable.ico_warning);
    }
    public void setCaptureStartMarker(FingerprintCaptureItem fp){
        ImageView nowMarker = fp.getFingerprintUI().getFingerprintMarker();
        nowMarker.setImageResource(R.drawable.down_arrow);
    }

    public void setStartCaptureFpView(FingerprintCaptureItem fp){

        ImageButton nowBtn = fp.getFingerprintUI().getFingerprintBtn();
        fp.getFingerprintUI().getFingerprintScore().setText("-");
        nowBtn.setImageResource(fp.getFingerprintID().getFpCaptureInitViewID());
        mFingerprintText.setText(R.string.capturing);

    }

    public void setFailedCaptureFpView(FingerprintCaptureItem fp){

        ImageButton nowBtn = fp.getFingerprintUI().getFingerprintBtn();
        fp.getFingerprintUI().getFingerprintScore().setText("-");
        nowBtn.setImageResource(fp.getFingerprintID().getFpCaptureFailedViewID());
        mFingerprintText.setText(R.string.fingerprint_capture_failed);
    }

    public void setFinishCaptureFpView(boolean lowScore , FingerprintCaptureItem fp){

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
    public void onCaptureStop(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
            synchronized (FingerprintCaptureActivity.this) {
                if (fp.getStatus() != FingerprintStatus.CAPTURED) {
                    fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                    fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                    resetMarker(fp);
                }

                mfpCaptureHandler.stopCapture();
            }
        });
    }
    @Override
    public void onCaptureStart(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {

            if(BuildConfig.isDebug) {
                Log.d("FaisalActivity", ">>>>> Entered in on onCaptureStart >>>> ");
            }
            synchronized (FingerprintCaptureActivity.this) {
                disableControls();

                mCurrentFingerprintCaptureItem = fp;
                mCurrentFingerprintCaptureItem.setStatus(FingerprintStatus.CAPTURE_IN_PROGRESS);

                setCaptureStartMarker(fp);
                setStartCaptureFpView(fp);
                startAnimation();
                Callable<Void> nowCallable = () -> {
                    mDeviceManager.startCapture();
                    return Void.TYPE.newInstance();
                };

                mFPStartCaptureService = Executors.newSingleThreadExecutor();
                mFPStartCaptureService.submit(nowCallable);

            }

        });

    }

    @Override
    public void onCaptureFailed(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
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
        });
    }

    @Override
    public void onCaptureEnd(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
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
        });
    }

    public void startAnimation(){
        ImageView imView = mCurrentFingerprintCaptureItem.getFingerprintUI().getFingerprintMarker();
        mCurrentAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom);
        imView.startAnimation(mCurrentAnimation);
    }

    @Override
    public void onFingerprintData(byte[] imgData, int width, int height,int score,long result) {
        try {


            long ret = -1;

            if (imgData != null && width > 0 && height > 0) {
                mDeviceManager.verifyFingerprint(imgData, width, height);

//                if(duplicateDetectionEnabled) {
//                    boolean[] matched = new boolean[1];
//                    ret = mfpMatchHandler.verifyFingerPrint(mCurrentFingerprintCaptureItem.getFingerprintID(), imgData, width, height, matched);
//
//
//                    if ((ret == 0) && matched[0]) {
//                        mFingerprintText.setText(R.string.duplicate_fingerprint);
//                        onCaptureError("Duplicate fingerprint");
//                        runOnUiThread(() -> Toast.makeText(FingerprintCaptureActivity.this,"Duplicate fingerprint captured. Please recapture different finger.",Toast.LENGTH_LONG).show());
//
//                        return;
//                    }
//                }
                byte[] wsqData = ImageProc.toWSQ(imgData, width, height);
                mfpCaptureHandler.setFingerprintData(mCurrentFingerprintCaptureItem.getFingerprintID(), score, wsqData);
                runOnUiThread(() -> {
                    try {
                        byte[] greyData = ImageProc.fromWSQ(mCurrentFingerprintCaptureItem.getFingerprintData().getFingerprintData(), width, height);
                        mFingerprintImage.setImageBitmap(ImageProc.toGrayscale(greyData, width, height));
                    } finally {
                        onCaptureEnd(mCurrentFingerprintCaptureItem);
                    }
                });
            }
        }catch(Exception exc){
            Log.d(TAG,exc.getMessage());
        }
    }
    @Override
    public void onFingerprintPreview(Bitmap img, int width, int height) {
        Log.d(TAG, "onFingerprintPreview() called with: img = [" + img + "], width = [" + width + "], height = [" + height + "]");
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
        if(cmd!=null && !cmd.isEmpty()){
            mFingerprintText.setText(cmd);
        }
    }

    @Override
    public void onCaptureError(String Error) {
        runOnUiThread(() -> {
            try {
                mFingerprintImage.setImageBitmap(ImageProc.createEmptyBitmap(248, 448));
            }finally {
                onCaptureFailed(mCurrentFingerprintCaptureItem);
            }
        });
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
        mOtherReasonTextView.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        });


        mOtherReasonTextView.setEnabled(false);
        mOtherReasonTextView.setBackgroundResource(R.drawable.border_disabled);
        mNoFingerprintReasonList = NoFingerprintReason.getReasonList();
        mHasFingerprintException = true;
        mNoFingerprintReason = NoFingerprintReason.getNoFingerPrintReasonByID(1);
        FingerprintExceptionListAdapter mAdapter = new FingerprintExceptionListAdapter(getApplicationContext(), mNoFingerprintReasonList);
        reasonSpinner.setAdapter(mAdapter);

        builder.setOnDismissListener(dialog -> {
            if (!mCloseClicked)
                FingerprintCaptureActivity.this.finish();
            else
                mCloseClicked = false;
        });

        builder.setCancelable(false);
        builder.setView(mView);
        AlertDialog dialog = builder.create();
        dialog.show();
        ok.setOnClickListener(v -> {

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
        });

        close.setOnClickListener(v -> {
            mCloseClicked = true;
            mHasFingerprintException = false;
            mNoFingerprintReason = null;
            mOtherReasonTextView = null;
            mNoFingerprintReasonList = null;
            dialog.dismiss();
        });
    }

    public void prepareReturnData() {


        List<FingerprintCaptureItem> capturedFingers = new ArrayList<>();


        Log.d(TAG, "prepareReturnData() called");
        Intent data = new Intent();
        try{

            data.putExtra("noFingerprint",mHasFingerprintException);

            if(BuildConfig.isDebug) {
                Log.d(TAG, "noFingerprint : " + mHasFingerprintException);
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

            for (FingerprintCaptureItem fp : mfpCaptureHandler.getFingerPrintList()) {
                if (fp.getFingerprintData().getFingerprintData() != null) {

                    int width = 256;
                    int height = 360;

                    FingerprintUtils utils = new FingerprintUtils();
                    ISOTemplate isoTemplate = utils.createISOTemplate(
                            fp.getFingerprintData().getFingerprintData(),
                            width, height
                    );

                    // ✅ Attach ISO Template to FingerprintData
                    fp.getFingerprintData().setIsoTemplate(isoTemplate);

                    capturedFingers.add(fp);

                    // ✅ Send FingerprintData with attached ISOTemplate
                    data.putExtra(fp.getFingerprintID().getName(), (Parcelable) fp.getFingerprintData());
                }
            }


            Log.d(TAG, "===== Captured Fingerprint Scores =====");
            for (FingerprintCaptureItem fp : capturedFingers) {
                String fingerName = fp.getFingerprintID().getName();
                long score = fp.getFingerprintData().getQualityScore();
                Log.d(TAG, fingerName + " -> Score: " + score);
            }
            Log.d(TAG, "========================================");

//
//            ExecutorService comparisonExecutor = Executors.newSingleThreadExecutor();
//            comparisonExecutor.execute(() -> {
//                long startTime = System.currentTimeMillis();
//                int totalComparisons = 0;
//
//                // 🔁 Timer thread to log every second
//                Thread timerThread = new Thread(() -> {
//                    int seconds = 0;
//                    try {
//                        while (!Thread.currentThread().isInterrupted()) {
//                            Thread.sleep(1000);
//                            seconds++;
//                            Log.d(TAG, "Matching Elapsed: " + seconds + " sec");
//                        }
//                    } catch (InterruptedException ignored) {
//                    }
//                });
//                timerThread.start();
//
//                // Precompute templates
//                Map<FingerprintID, FingerprintTemplate> templateMap = new HashMap<>();
//                for (Fingerprint fp : capturedFingers) {
//                    FingerprintTemplate template = new FingerprintTemplate()
//                            .dpi(500)
//                            .create(fp.getFingerprintData().getFingerprintData());
//                    templateMap.put(fp.getFingerprintID(), template);
//                }
//
//                for (int i = 0; i < capturedFingers.size(); i++) {
//                    Fingerprint f1 = capturedFingers.get(i);
//                    FingerprintTemplate source = templateMap.get(f1.getFingerprintID());
//                    FingerprintMatcher matcher = new FingerprintMatcher().index(source);
//
//                    for (int j = 0; j < capturedFingers.size(); j++) {
//                        if (i == j) continue;
//                        Fingerprint f2 = capturedFingers.get(j);
//                        FingerprintTemplate target = templateMap.get(f2.getFingerprintID());
//                        double score = matcher.match(target);
//                        totalComparisons++;
//
//                        Log.d(TAG, "Comparing " + f1.getFingerprintID().getName() +
//                                " vs " + f2.getFingerprintID().getName() +
//                                " -> Score: " + score +
//                                (score >= 40 ? " ✅ MATCH" : " ❌ NO MATCH"));
//                    }
//                }
//
//                // Stop the timer thread
//                timerThread.interrupt();
//
//                long endTime = System.currentTimeMillis();
//                long durationMs = endTime - startTime;
//                long secondsTaken = (durationMs / 1000) % 60;
//                long minutesTaken = (durationMs / 1000) / 60;
//
//                Log.d(TAG, "===== Fingerprint Similarity Check Completed =====");
//                Log.d(TAG, "Total Comparisons: " + totalComparisons);
//                Log.d(TAG, "Time Taken: " + minutesTaken + " min " + secondsTaken + " sec");
//            });



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

    private void showNoAccessToDevice(){
        runOnUiThread(() -> {
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(FingerprintCaptureActivity.this).create();
            alertDialog.setCancelable(false);
            alertDialog.setTitle(R.string.app_name);
            alertDialog.setMessage(getString(R.string.noAccessToDevice));
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", (dialogInterface, i) -> FingerprintCaptureActivity.this.finish());
            alertDialog.show();
        });
    }

    private void showFingerprintDeviceNotInitialized(long result){
        AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
        dlgAlert.setMessage("Fingerprint device open failed with error : " + result);
        dlgAlert.setTitle("Fingerprint Registration");
        dlgAlert.setPositiveButton("OK",
                (dialog, whichButton) -> finish()
        );
        dlgAlert.setCancelable(false);
        dlgAlert.create().show();
    }


}