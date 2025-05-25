package com.kit.photocapture.presentation.activity;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.kit.photocapture.util.faceDetectionUtils.FrameStabilityUtils.isSceneStable;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;

import com.kit.biometricsdk.R;
import com.kit.photocapture.detector.FaceDetector;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.util.faceDetectionUtils.DrawingUtils;
import com.kit.photocapture.util.faceDetectionUtils.FaceComplianceUtils;
import com.kit.photocapture.util.faceDetectionUtils.FrameStabilityUtils;
import com.kit.photocapture.view.BoxOverlayView;

public class PhotoCaptureActivity2 extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {


    private boolean facePreviouslyDetected = false;
    private boolean faceImageSaved = false;
    private boolean noFaceImageSaved = false;
    private static final String TAG = "PhotoCaptureActivity";

    private Mat mRgb;
    private Mat mResized;

    private CameraBridgeViewBase mOpenCvCameraView;
    private BoxOverlayView mBoxOverlay;
    private ImageButton mCapture;
    private ImageButton mSwitchCamera;
    private TextView mStatusText;

    private Mat mRgba_CurrentFrame;
    private FaceDetector mFaceDetector;
    private Bitmap mResultBmp = null;
    private Uri mResultUri = null;
    private boolean mIsFrontCamera;


    private Mat previousFrame = null;


    private int as_usual_frameCount = 0;
    private int frameAfterToCheck = 1;


    private boolean isFrameStable = false;


    private int stableFrameCount = 0;
    private static final int REQUIRED_CONSECUTIVE_STABLE_FRAMES_TO_SHOW_BOX = 5;
    private boolean isBoxVisible = false;


//    private boolean lastStabilityResult = false;

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    initializeFaceDetector();
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    Log.e(TAG, "OpenCV loading failed");
                    Toast.makeText(PhotoCaptureActivity2.this,
                            "OpenCV initialization failed", Toast.LENGTH_LONG).show();
                    finish();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_photo_capture);
        initializeViews();

    }



    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {


       // as_usual_frameCount++;

       //  Frame Preparation to suit the model and to show in the display
        mRgba_CurrentFrame = inputFrame.rgba();
        if (mIsFrontCamera) {Core.flip(mRgba_CurrentFrame, mRgba_CurrentFrame, 1);}         // Flip front camera (mirror)
        Mat mRgba_DetectionFrame = mRgba_CurrentFrame.clone();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.rotate(mRgba_DetectionFrame, mRgba_DetectionFrame, Core.ROTATE_90_CLOCKWISE);
        }




        isFrameStable = checkAndTrackFrameStability(mRgba_CurrentFrame);

        if (isFrameStable) {
            handleStableFrame_ToShowBox();
            processFaceInBox(mRgba_DetectionFrame);

        } else {
            handleUnstableFrame();
        }

        return mRgba_CurrentFrame;
    }



    private void processFaceInBox(Mat detectionInput) {
        if (!isBoxVisible) return;

        android.graphics.Rect screenBox = mBoxOverlay.getBoxRect();

        Rect roi = mapBoxRectToOpenCV(screenBox, detectionInput.size());
        roi = adjustRectToBounds(roi, detectionInput.cols(), detectionInput.rows());

        Mat faceRegionRgba = new Mat(detectionInput, roi);
        Mat faceRegionRgb = new Mat();
        Imgproc.cvtColor(faceRegionRgba, faceRegionRgb, Imgproc.COLOR_RGBA2RGB);

        mFaceDetector.setInputSize(new Size(roi.width, roi.height));

        Mat faces = new Mat();
        mFaceDetector.detect(faceRegionRgb, faces);

        int faceCount = faces.rows();

        if (faceCount == 1) {
            handleSingleFace(faces, detectionInput, roi);
        } else if (faceCount > 1) {
            handleMultipleFaces();
        } else {
            handleNoFace();
        }

        faces.release();
        faceRegionRgb.release();
        faceRegionRgba.release();

        Log.d("FaceDetection", "👤 Face count detected in box: " + faceCount);
    }

    private void handleStableFrame_ToShowBox() {
        stableFrameCount++;
        Log.d("anik02", "✅ Stable frame count: " + stableFrameCount);

//        mCapture.setEnabled(false);
//        mCapture.setVisibility(View.INVISIBLE);
        if (stableFrameCount >= REQUIRED_CONSECUTIVE_STABLE_FRAMES_TO_SHOW_BOX) {
            stableFrameCount = REQUIRED_CONSECUTIVE_STABLE_FRAMES_TO_SHOW_BOX;
            if (!isBoxVisible) {
                runOnUiThread(() -> {
                    mBoxOverlay.setVisibility(VISIBLE);
                    Log.d("anik02", "🎯 Box is now visible");
                });
                isBoxVisible = true;
            }

        }
    }




    private void handleNoFace() {
        runOnUiThread(() -> {
            mStatusText.setText("No face detected - please position your face in the frame");
            mBoxOverlay.setBoxState(BoxOverlayView.BoxState.RED);
            mCapture.setEnabled(false);
            mCapture.setVisibility(View.INVISIBLE);
        });
    }

    private void handleSingleFace(Mat faces,Mat detectionInput,Rect roi) {

        DrawingUtils.drawFacialKeypoints(
                faces, roi, detectionInput, mRgba_CurrentFrame,
                Core.ROTATE_90_CLOCKWISE, mIsFrontCamera
        );



        boolean compliant = FaceComplianceUtils.isFaceCompliant(faces, roi);


        if(compliant){

            runOnUiThread(() -> {
                mBoxOverlay.setBoxState(BoxOverlayView.BoxState.GREEN);
                mStatusText.setText("Perfect !!");
                mCapture.setVisibility(View.VISIBLE);
                mCapture.setEnabled(true);
            });

        }else{

            runOnUiThread(() -> {
                mBoxOverlay.setBoxState(BoxOverlayView.BoxState.YELLOW);
                mStatusText.setText("Center your face in the frame");
                mCapture.setEnabled(false);
                mCapture.setVisibility(View.INVISIBLE);
            });
        }
//        mBoxOverlay.setBoxState(compliant
//                ? BoxOverlayView.BoxState.GREEN
//                : BoxOverlayView.BoxState.YELLOW);

/*                    if (compliant) {
                        double[] row = new double[15];
                        for (int i = 0; i < 15; i++) {
                            double[] value = faces.get(0, i);
                            row[i] = (value != null && value.length > 0) ? value[0] : 0.0;
                        }
                    }*/




//
//        double[] x = faces.get(0, 0); // This should return 15 values: [x, y, w, h, eye1_x, eye1_y, ... score]
//        double[] y = faces.get(0, 1);
//        double[] w = faces.get(0, 2);
//        double[] h = faces.get(0, 3);

//        Rect faceRect = new Rect((int)x[0], (int)y[0], (int)w[0], (int)h[0]);

//        if (!isWithinComplianceBox(faceRect)) {
//            runOnUiThread(() -> {
//                mStatusText.setText("Center your face in the frame");
//                mBoxOverlay.setCompliant(false);
//                mCapture.setEnabled(false);
//            });
//            return;
//        }

//        ICAOComplianceUtils.checkICAOCompliance(
//                faces,
//                new Size(mRgb.cols(), mRgb.rows()),
//                (compliant, message) -> runOnUiThread(() -> {
//                    mStatusText.setText(message);
//                    mBoxOverlay.setCompliant(compliant);
//                    mCapture.setEnabled(compliant);
//
//                    if (compliant) {
//                        Imgproc.rectangle(mRgb,
//                                faceRect.tl(), faceRect.br(),
//                                new Scalar(0, 255, 0), 3);
//                    }
//                })
//        );
    }

    private void handleMultipleFaces() {
        runOnUiThread(() -> {
            mStatusText.setText("Multiple faces detected - only one person should be in frame");
//            mBoxOverlay.setCompliant(false);
            mCapture.setEnabled(false);
            mCapture.setVisibility(View.INVISIBLE);
        });
    }

    private void handleUnstableFrame() {
        Log.d("anik01", "❌ Frame unstable — resetting stability count");

        mCapture.setEnabled(false);
        mCapture.setVisibility(View.INVISIBLE);

        if (isBoxVisible) {
            runOnUiThread(() -> {
                mBoxOverlay.setVisibility(INVISIBLE);
                Log.d("anik01", "🚫 Box is hidden due to instability");
            });
            isBoxVisible = false;
        }
        stableFrameCount = 0; // dropping the value to start from zero
    }


    private boolean checkAndTrackFrameStability(Mat currentFrame) {
        FrameStabilityUtils.StabilityResult frameStabilityResult = isSceneStable(currentFrame, previousFrame);
        previousFrame = frameStabilityResult.updatedPreviousFrame;
        return frameStabilityResult.isStable;
    }




    private Rect adjustRectToBounds(Rect roi, int frameWidth, int frameHeight) {
        int x = Math.max(0, roi.x);
        int y = Math.max(0, roi.y);
        int width = Math.min(roi.width, frameWidth - x);
        int height = Math.min(roi.height, frameHeight - y);
        return new Rect(x, y, width, height);
    }

    private Rect mapBoxRectToOpenCV(android.graphics.Rect screenBox, Size frameSize) {
        float scaleX = (float) frameSize.width / mBoxOverlay.getWidth();
        float scaleY = (float) frameSize.height / mBoxOverlay.getHeight();

        int left = Math.round(screenBox.left * scaleX);
        int top = Math.round(screenBox.top * scaleY);
        int width = Math.round(screenBox.width() * scaleX);
        int height = Math.round(screenBox.height() * scaleY);

        return new Rect(left, top, width, height);
    }



    private android.graphics.Rect mapBoxRectToResizedInput(android.graphics.Rect boxOnScreen, Size originalSize, Size resizedSize) {
        float xScale = (float) resizedSize.width / (float) originalSize.width;
        float yScale = (float) resizedSize.height / (float) originalSize.height;

        int left = Math.round(boxOnScreen.left * xScale);
        int top = Math.round(boxOnScreen.top * yScale);
        int right = Math.round(boxOnScreen.right * xScale);
        int bottom = Math.round(boxOnScreen.bottom * yScale);

        return new android.graphics.Rect(left, top, right, bottom);
    }

    private boolean isWithinComplianceBox(Rect faceRect) {
        android.graphics.Rect boxRect = mBoxOverlay.getBoxRect();
        if (boxRect == null || mRgba_CurrentFrame == null) return false;

        // Scale OpenCV face rect to view space
        float scaleX = (float) boxRect.width() / mRgba_CurrentFrame.cols();
        float scaleY = (float) boxRect.height() / mRgba_CurrentFrame.rows();

        int left = (int) (faceRect.x * scaleX);
        int top = (int) (faceRect.y * scaleY);
        int right = (int) ((faceRect.x + faceRect.width) * scaleX);
        int bottom = (int) ((faceRect.y + faceRect.height) * scaleY);

        return boxRect.contains(left, top) && boxRect.contains(right, bottom);
    }

    private void initializeFaceDetector() {
        try {
            mFaceDetector = new YunetFaceDetectionImpl(PhotoCaptureActivity2.this);
            mFaceDetector.loadDetector();
            Log.i(TAG, "Face detector initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing face detector", e);
            Toast.makeText(this, "Error initializing face detector", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void onCapture() {
        if (mRgba_CurrentFrame != null && !mRgba_CurrentFrame.empty()) {
            try {
                Bitmap bmp = Bitmap.createBitmap(mRgba_CurrentFrame.cols(), mRgba_CurrentFrame.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(mRgba_CurrentFrame, bmp);
                mResultBmp = bmp;
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
                // TODO: Process or save the captured image
            } catch (Exception e) {
                Log.e(TAG, "Error capturing image", e);
                Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
            }
        }
    }







    private void initializeViews() {
        mStatusText = findViewById(R.id.status_text);
        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setMaxFrameSize(1280, 720); // Limit resolution

        mIsFrontCamera = mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT;

        mBoxOverlay = findViewById(R.id.box_overlay);

        mCapture = findViewById(R.id.capture_button);
        mCapture.setVisibility(View.INVISIBLE);
        mCapture.setEnabled(false);
        mCapture.setOnClickListener(v -> onCapture());

        mSwitchCamera = findViewById(R.id.switch_button);
        mSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    private void switchCamera() {
        if (mOpenCvCameraView != null) {
            int currentCamera = mOpenCvCameraView.getCameraIndex();
            int newCamera = (currentCamera == CameraBridgeViewBase.CAMERA_ID_BACK) ?
                    CameraBridgeViewBase.CAMERA_ID_FRONT :
                    CameraBridgeViewBase.CAMERA_ID_BACK;
            mOpenCvCameraView.disableView();
            mIsFrontCamera = newCamera == CameraBridgeViewBase.CAMERA_ID_FRONT;
            mOpenCvCameraView.setCameraIndex(newCamera);
            mOpenCvCameraView.enableView();
        }
    }


    private void releaseResources() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (mRgba_CurrentFrame != null) {
            mRgba_CurrentFrame.release();
        }
        if (mRgb != null) {
            mRgb.release();
        }
        if (mResized != null) {
            mResized.release();
        }
        if (mFaceDetector != null) {
            mFaceDetector = null;
        }
    }



    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba_CurrentFrame = new Mat();
        mRgb = new Mat();
        mResized = new Mat(); // ✅ initialize here
    }

    @Override
    public void onCameraViewStopped() {
        if (mRgba_CurrentFrame != null) {
            mRgba_CurrentFrame.release();
            mRgba_CurrentFrame = null;
        }
        if (mRgb != null) {
            mRgb.release();
            mRgb = null;
        }
        if (mResized != null) {
            mResized.release();
            mResized = null;
        }
    }



    @Override
    protected void onResume() {
        super.onResume();
        if (!OpenCVLoader.initLocal()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager");
        } else {
            Log.d(TAG, "OpenCV library found inside package");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseResources();
    }


}