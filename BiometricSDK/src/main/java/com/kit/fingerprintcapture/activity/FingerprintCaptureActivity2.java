package com.kit.fingerprintcapture.activity;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.dermalog.common.exception.ErrorCodes;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.manager.DummyDeviceManager;
import com.kit.fingerprintcapture.manager.MorphoDeviceManager;
import com.kit.fingerprintcapture.model.FingerprintCaptureItem;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.kit.fingerprintcapture.viewmodel.FingerprintCaptureViewModel;

import java.util.ArrayList;
import java.util.Objects;

public class FingerprintCaptureActivity2 extends AppCompatActivity implements DeviceDataCallback, View.OnClickListener {
    public static final String TAG = "FingerprintCaptureActivity2";

    private ImageView mFingerprintImage;
    private TextView mFingerprintText;
    private TextView mClickFingerprint;

    private FingerprintCaptureItem mCurrentFingerprintCaptureItem;

    private Button mDoneBtn;

    FingerprintCaptureViewModel viewModel;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        setContentView(R.layout.fingerprint_capture_layout);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        viewModel = new ViewModelProvider(this).get(FingerprintCaptureViewModel.class);


        setupUI();
    }




    private void setupUI(){

         ArrayList<FingerprintCaptureItem> fingerprintCaptureItemList = new ArrayList<>();


        mFingerprintImage = findViewById(R.id.fingerprint_image);
        mFingerprintText = findViewById(R.id.fingerprint_text);
        mClickFingerprint = findViewById(R.id.click_fingerprint);

        mDoneBtn = findViewById(R.id.doneBtn);

        FingerprintCaptureItem fPrint = FingerprintCaptureItem.newInstance(getWindow().getDecorView(), FingerprintID.RIGHT_THUMB,R.id.right_thumb,R.id.right_thumb_marker,R.id.right_thumb_score_text);
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
            fp.getFingerprintUI().getFingerprintBtn().setOnClickListener(this);
        }

        viewModel.fingerprintList.setValue(fingerprintCaptureItemList);
    }




    @Override
    public void onStart(){
        super.onStart();
        if(viewModel.isDummyDevice()){

            viewModel.setmDeviceManager(new DummyDeviceManager(this,this));
        } else {
            viewModel.setmDeviceManager(new MorphoDeviceManager(this,this));
            Log.d(TAG, "morpho    ");
        }
    }




    @Override
    public void onResume(){
        super.onResume();

        disableControls();

        mFingerprintText.setVisibility(View.INVISIBLE);
        mClickFingerprint.setText(R.string.device_initizing);

        try {
            long result = viewModel.getmDeviceManager().initDevice();

            if(BuildConfig.isDebug){
                Log.d(TAG, "initDevice() returned : " + result);
            }

            if(result!= ErrorCodes.FPC_SUCCESS){
                showNoAccessToDevice();
                return;
            }else{
                result = viewModel.getmDeviceManager().openDevice();
                if(result != ErrorCodes.FPC_SUCCESS) {
                    showFingerprintDeviceNotInitialized(result);
                }else{
                    mFingerprintText.setVisibility(View.VISIBLE);
                    mClickFingerprint.setText(R.string.click_fingerprint);

                    if(!viewModel.isDummyDevice()) {
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
    public void onClick(View v) {
        int currentId = v.getId();

        for(FingerprintCaptureItem item : Objects.requireNonNull(viewModel.fingerprintList.getValue())){
            if(currentId == item.getFingerprintUI().getFingerprintBtn().getId()){
                onCaptureStart(item);
            }
        }

    }
    public void onCaptureStart(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {

            disableControls();

            mCurrentFingerprintCaptureItem = fp;

            fp.setStatus(FingerprintStatus.CAPTURE_IN_PROGRESS);

            setCaptureStartMarker(fp);
            setStartCaptureFpView(fp);
            startAnimation(fp);
            viewModel.getmDeviceManager().startCapture();

        });
    }

    public void onCaptureEnd(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
            try {
                fp.setStatus(FingerprintStatus.CAPTURED);

                boolean lowQuality = fp.getFingerprintData().getQualityScore() < 50;

                setCaptureFinishedMarker(lowQuality, fp);
                setFinishCaptureFpView(lowQuality, fp);
                fp.getFingerprintUI().getFingerprintMarker().clearAnimation();

                // ✅ Show toast
                Toast.makeText(
                        this,  // Replace with a valid context, e.g., `this` if inside Activity
                        lowQuality ? "Fingerprint captured, but quality is low!"+ fp.getFingerprintData().getQualityScore() : "Fingerprint captured successfully.",
                        Toast.LENGTH_SHORT
                ).show();

            } finally {
                enableControls();
            }
        });
    }


    public void onCaptureStop(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
            if (fp.getStatus() != FingerprintStatus.CAPTURED) {
                fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
                fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                resetMarker(fp);
            }
        });
    }

    public void onCaptureFailed(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
            try {
                fp.setStatus(FingerprintStatus.NOT_CAPTURED);
                setCaptureFailedMarker(fp);
                setFailedCaptureFpView(fp);
                fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
            } finally {
                enableControls();
            }
        });
    }


    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }

    public void startAnimation(FingerprintCaptureItem item){
        ImageView imView = item.getFingerprintUI().getFingerprintMarker();
        imView.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom));
    }

    public void disableControls(){
        mDoneBtn.setEnabled(false);
        for(FingerprintCaptureItem fp : Objects.requireNonNull(viewModel.fingerprintList.getValue())){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(false);
        }
    }
    public void enableControls(){
        mDoneBtn.setEnabled(true);
        for(FingerprintCaptureItem fp : Objects.requireNonNull(viewModel.fingerprintList.getValue())){
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

    /// Code for processing Fingerprint
    @Override
    public void onFingerprintData(byte[] imgData, int width, int height, int qualityScore, long captureResult) {
        try {


            long ret = -1;

            if (imgData != null && width > 0 && height > 0) {
                viewModel.getmDeviceManager().verifyFingerprint(imgData, width, height);

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
                viewModel.setFingerprintData(mCurrentFingerprintCaptureItem, qualityScore, wsqData);
                runOnUiThread(() -> {
                    try {
                        byte[] greyData = ImageProc.fromWSQ(mCurrentFingerprintCaptureItem.getFingerprintData().getFingerprintData(), width, height);
                      //  mFingerprintImage.setImageBitmap(ImageProc.toGrayscale(greyData, width, height));
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



    private void showNoAccessToDevice(){
        runOnUiThread(() -> {
            android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(FingerprintCaptureActivity2.this).create();
            alertDialog.setCancelable(false);
            alertDialog.setTitle(R.string.app_name);
            alertDialog.setMessage(getString(R.string.noAccessToDevice));
            alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", (dialogInterface, i) -> FingerprintCaptureActivity2.this.finish());
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
