package com.kit.photocapture.presentation.activity;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import static com.kit.photocapture.util.faceDetectionUtils.FrameStabilityUtils.isSceneStable;

import android.content.Intent;
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

import androidx.annotation.Nullable;

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

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.kit.biometricsdk.R;
import com.kit.photocapture.detector.FaceDetector;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.util.ICAOComplianceUtils;
import com.kit.photocapture.util.faceDetectionUtils.ComplianceResult;
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
    private FloatingActionButton mCapture;
    private FloatingActionButton mSwitchCamera;
    private TextView mStatusText;

    private Mat mRgbaCurrentFrame;
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


    private Mat mCleanCaptureFrame = null;



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
        mRgbaCurrentFrame = inputFrame.rgba();
        if (mIsFrontCamera) {Core.flip(mRgbaCurrentFrame, mRgbaCurrentFrame, 1);}         // Flip front camera (mirror)
        Mat mRgbaDetectionFrame = mRgbaCurrentFrame.clone();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.rotate(mRgbaDetectionFrame, mRgbaDetectionFrame, Core.ROTATE_90_CLOCKWISE);
        }




        isFrameStable = checkAndTrackFrameStability(mRgbaCurrentFrame);

        if (isFrameStable) {
            showRectrangleBox();
            processFaceInBox(mRgbaDetectionFrame);

        } else {
            handleUnstableFrame();
        }

        return mRgbaCurrentFrame;
    }



    private void processFaceInBox(Mat detectionInputFrame) {
        if (!isBoxVisible) return;

        // Get the coordinates of the box drawn on the screen (UI coordinates)
        android.graphics.Rect screenBox = mBoxOverlay.getBoxRect();



        //This lets you isolate and analyze the exact part of the image the user sees inside the on-screen guide box.
        Rect roi = mapBoxRectToOpenCV(screenBox, detectionInputFrame.size());
        roi = adjustRectToBounds(roi, detectionInputFrame.cols(), detectionInputFrame.rows());



        /*
         * Extract the region of interest (ROI) from the current detection frame using the rectangle (roi),
         * which corresponds to the on-screen guide box.
         *
         * Then convert this cropped region from RGBA to RGB format, since the face detector expects RGB input.
         *
         * Set the input size of the face detector to match the size of the cropped region.
         * This ensures the model scales or adjusts its detection logic correctly.
         *
         * Finally, run the face detector on the converted RGB region to detect faces within the guide box.
         * The detection result (if any) will be stored in the 'faces' Mat.
         */

        Mat faceRegionRgba = new Mat(detectionInputFrame, roi);
        Mat faceRegionRgb = new Mat();

        Imgproc.cvtColor(faceRegionRgba, faceRegionRgb, Imgproc.COLOR_RGBA2RGB);

        mFaceDetector.setInputSize(new Size(roi.width, roi.height));

        Mat faces = new Mat();
        mFaceDetector.detect(faceRegionRgb, faces);


        int faceCount = faces.rows();

        if (faceCount == 1) {
            handleSingleFace(faces, detectionInputFrame, roi);
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



    private Rect adjustRectToBounds(Rect roi, int frameWidth, int frameHeight) {
        int x = Math.max(0, roi.x);
        int y = Math.max(0, roi.y);
        int width = Math.min(roi.width, frameWidth - x);
        int height = Math.min(roi.height, frameHeight - y);
        return new Rect(x, y, width, height);
    }



   /*
   * This is necessary because screen coordinates and camera frame sizes may differ
   * due to scaling, rotation, or resolution differences.
   */
    private Rect mapBoxRectToOpenCV(android.graphics.Rect screenBox, Size frameSize) {
        float scaleX = (float) frameSize.width / mBoxOverlay.getWidth();
        float scaleY = (float) frameSize.height / mBoxOverlay.getHeight();

        int left = Math.round(screenBox.left * scaleX);
        int top = Math.round(screenBox.top * scaleY);
        int width = Math.round(screenBox.width() * scaleX);
        int height = Math.round(screenBox.height() * scaleY);

        return new Rect(left, top, width, height);
    }




    // basic camera and face detector methods
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
        if (mCleanCaptureFrame != null && !mCleanCaptureFrame.empty()) {
            try {
                // Get the guide box rectangle (in screen pixels)
                android.graphics.Rect screenBox = mBoxOverlay.getBoxRect();

                // Map screenBox to OpenCV coordinates
                Rect roi = mapBoxRectToOpenCV(screenBox, mCleanCaptureFrame.size());
                roi = adjustRectToBounds(roi, mCleanCaptureFrame.cols(), mCleanCaptureFrame.rows());

                // Extract face region
                Mat croppedFace = new Mat(mCleanCaptureFrame, roi);

                // Resize face region to match box pixel size (same as UI box size)
                Mat resizedFace = new Mat();
                Size targetSize = new Size(screenBox.width(), screenBox.height());
                Imgproc.resize(croppedFace, resizedFace, targetSize);

                // Convert Mat to Bitmap
                Bitmap bmp = Bitmap.createBitmap(resizedFace.cols(), resizedFace.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(resizedFace, bmp);

                // Save the bitmap to cache
                File file = new File(getCacheDir(), "captured_photo.jpg");
                FileOutputStream out = new FileOutputStream(file);
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();

                // Launch preview activity
                Intent intent = new Intent(this, CapturedPhotoPreviewActivity.class);
                intent.putExtra(CapturedPhotoPreviewActivity.EXTRA_PHOTO_PATH, file.getAbsolutePath());
                startActivityForResult(intent, 201); // use a request code to capture the result


              //  finish();

                // Cleanup
                croppedFace.release();
                resizedFace.release();

            } catch (Exception e) {
                Log.e(TAG, "Error capturing image", e);
                Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 201 && resultCode == RESULT_OK && data != null) {
            // Forward result back to FaceMatchActivity
            setResult(RESULT_OK, data);
            finish();
        }
    }



//    private void onCapture() {
//        if (mCleanCaptureFrame != null && !mCleanCaptureFrame.empty()) {
//            try {
//                Bitmap bmp = Bitmap.createBitmap(
//                        mCleanCaptureFrame.cols(),
//                        mCleanCaptureFrame.rows(),
//                        Bitmap.Config.ARGB_8888
//                );
//                Utils.matToBitmap(mCleanCaptureFrame, bmp);
//
//                File file = new File(getCacheDir(), "captured_photo.jpg");
//                FileOutputStream out = new FileOutputStream(file);
//                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out);
//                out.close();
//
//                Intent intent = new Intent(this, CapturedPhotoPreviewActivity.class);
//                intent.putExtra(CapturedPhotoPreviewActivity.EXTRA_PHOTO_PATH, file.getAbsolutePath());
//                startActivity(intent);
//
//            } catch (Exception e) {
//                Log.e(TAG, "Error capturing image", e);
//                Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
//            }
//        }
//    }

    private void initializeViews() {
        mStatusText = findViewById(R.id.status_text);
        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
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
        if (mRgbaCurrentFrame != null) {
            mRgbaCurrentFrame.release();
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
        mRgbaCurrentFrame = new Mat();
        mRgb = new Mat();
        mResized = new Mat(); // ✅ initialize here
    }

    @Override
    public void onCameraViewStopped() {
        if (mRgbaCurrentFrame != null) {
            mRgbaCurrentFrame.release();
            mRgbaCurrentFrame = null;
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




    //handle faces

    private void handleNoFace() {
        runOnUiThread(() -> {
            mStatusText.setText("No face detected - please position your face in the frame");
            mBoxOverlay.setBoxState(BoxOverlayView.BoxState.RED);
            mCapture.setEnabled(false);
            mCapture.setVisibility(View.INVISIBLE);
        });
    }

    private void handleSingleFace(Mat faces,Mat detectionInput,Rect roi) {


        // Clone a clean frame before drawing landmarks
        mCleanCaptureFrame = detectionInput.clone();

        int rotation = Core.ROTATE_90_CLOCKWISE;
        if (!mIsFrontCamera) {
            // Optionally: apply ROTATE_90_CLOCKWISE only if portrait orientation
            rotation = Core.ROTATE_90_CLOCKWISE; // or ROTATE_90_COUNTERCLOCKWISE if landmarks appear flipped
        }

        DrawingUtils.drawFacialKeypoints(
                faces, roi, detectionInput, mRgbaCurrentFrame,
                rotation, mIsFrontCamera
        );


        ComplianceResult compliantResult = FaceComplianceUtils.isFaceCompliant(faces, roi);


        if (compliantResult.isCompliant) {
            ICAOComplianceUtils.checkICAOCompliance(faces, new Size(detectionInput.cols(), detectionInput.rows()), (isCompliant, message) -> {
                runOnUiThread(() -> {
                    if (isCompliant) {
                        mBoxOverlay.setBoxState(BoxOverlayView.BoxState.GREEN);
                        mStatusText.setVisibility(View.INVISIBLE);
//                        mStatusText.setText("Perfect !!\n" + message);
                        mCapture.setVisibility(View.VISIBLE);
                        mCapture.setEnabled(true);
                     //   Toast.makeText(this, "✅ ICAO Passed: " + message, Toast.LENGTH_SHORT).show();
                    } else {
                        mBoxOverlay.setBoxState(BoxOverlayView.BoxState.YELLOW);
                        mStatusText.setVisibility(VISIBLE);
                        mStatusText.setText( message);//"ICAO Issue: " +
                        mCapture.setEnabled(false);
                        mCapture.setVisibility(View.INVISIBLE);
                      //  Toast.makeText(this, "❌ ICAO Failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else{

            runOnUiThread(() -> {
                mBoxOverlay.setBoxState(BoxOverlayView.BoxState.YELLOW);
                mStatusText.setVisibility(VISIBLE);
                mStatusText.setText(compliantResult.message);
                mCapture.setEnabled(false);
                mCapture.setVisibility(View.INVISIBLE);
            });
        }

    }

    private void handleMultipleFaces() {
        runOnUiThread(() -> {
            mBoxOverlay.setBoxState(BoxOverlayView.BoxState.YELLOW);
            mStatusText.setVisibility(VISIBLE);
            mStatusText.setText("Multiple faces detected - only one person should be in frame");
//            mBoxOverlay.setCompliant(false);
            mCapture.setEnabled(false);
            mCapture.setVisibility(View.INVISIBLE);
        });
    }




    //handle frame stablity
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



    private void showRectrangleBox() {
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



}