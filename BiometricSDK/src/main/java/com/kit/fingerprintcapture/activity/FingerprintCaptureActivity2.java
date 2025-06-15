package com.kit.fingerprintcapture.activity;

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

import androidx.appcompat.app.AppCompatActivity;

import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.model.FingerprintCaptureItem;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.model.FingerprintStatus;
import com.kit.fingerprintcapture.utils.ImageProc;

import java.util.ArrayList;

public class FingerprintCaptureActivity2 extends AppCompatActivity implements DeviceDataCallback, View.OnClickListener {
    public static final String TAG = "FingerprintCaptureActivity2";

    private ImageView mFingerprintImage;
    private TextView mFingerprintText;
    private TextView mClickFingerprint;

    private FingerprintCaptureItem mCurrentFingerprintCaptureItem;

    private Button mDoneBtn;

    private ArrayList<FingerprintCaptureItem> fingerprintCaptureItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        setContentView(R.layout.fingerprint_capture_layout);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        setupUI();
    }

    private void setupUI(){

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
    }


    @Override
    public void onClick(View v) {
        int currentId = v.getId();

        for(FingerprintCaptureItem item : fingerprintCaptureItemList){
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
//            mDeviceManager.startCapture();

        });
    }

    public void onCaptureEnd(FingerprintCaptureItem fp) {
        runOnUiThread(() -> {
            try {
                fp.setStatus(FingerprintStatus.CAPTURED);
                setCaptureFinishedMarker(fp.getFingerprintData().getQualityScore() < 50, fp);
                setFinishCaptureFpView(fp.getFingerprintData().getQualityScore() < 50, fp);
                fp.getFingerprintUI().getFingerprintMarker().clearAnimation();
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
        for(FingerprintCaptureItem fp : fingerprintCaptureItemList){
            fp.getFingerprintUI().getFingerprintBtn().setEnabled(false);
        }
    }
    public void enableControls(){
        mDoneBtn.setEnabled(true);
        for(FingerprintCaptureItem fp : fingerprintCaptureItemList){
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

}
