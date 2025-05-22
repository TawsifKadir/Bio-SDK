package com.kit.photocapture.activity;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
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
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.kit.biometricsdk.R;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.detector.FaceDetector;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.result.ComplianceResult;
import com.kit.photocapture.util.ICAOComplianceUtils;
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

    private Mat mRgba;
    private FaceDetector mFaceDetector;
    private Bitmap mResultBmp = null;
    private Uri mResultUri = null;
    private boolean mIsFrontCamera;

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

    private void initializeViews() {
        mStatusText = findViewById(R.id.status_text);
        mOpenCvCameraView = findViewById(R.id.camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setMaxFrameSize(1280, 720); // Limit resolution

        mIsFrontCamera = mOpenCvCameraView.getCameraIndex() == CameraBridgeViewBase.CAMERA_ID_FRONT;

        mBoxOverlay = findViewById(R.id.box_overlay);

        mCapture = findViewById(R.id.capture_button);
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

    private void releaseResources() {
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
        if (mRgba != null) {
            mRgba.release();
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
        mRgba = new Mat();
        mRgb = new Mat();
        mResized = new Mat(); // ✅ initialize here
    }

    @Override
    public void onCameraViewStopped() {
        if (mRgba != null) {
            mRgba.release();
            mRgba = null;
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


    private static final double HISTOGRAM_CORRELATION_THRESHOLD = 0.92; // 1.0 = identical
    private static final int HIST_SIZE = 64; // Number of histogram bins
    private static final float[] HIST_RANGE = {0f, 256f}; // Grayscale range
    private Mat mPrevGray = null;
    private boolean isSceneStable(Mat currentRgba) {
        Mat currGray = new Mat();
        Imgproc.cvtColor(currentRgba, currGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(currGray, currGray, new Size(3, 3), 0);

        if (mPrevGray == null) {
            mPrevGray = currGray.clone();
            Log.d("anik", "🔄 First frame, skipping stability check.");
            return false;
        }

        // Compute histogram for current frame
        Mat currHist = new Mat();
        MatOfInt histSize = new MatOfInt(HIST_SIZE);
        MatOfFloat ranges = new MatOfFloat(HIST_RANGE);
        Imgproc.calcHist(Arrays.asList(currGray), new MatOfInt(0), new Mat(), currHist, histSize, ranges);
        Core.normalize(currHist, currHist, 0, 1, Core.NORM_MINMAX);

        // Compute histogram for previous frame
        Mat prevHist = new Mat();
        Imgproc.calcHist(Arrays.asList(mPrevGray), new MatOfInt(0), new Mat(), prevHist, histSize, ranges);
        Core.normalize(prevHist, prevHist, 0, 1, Core.NORM_MINMAX);

        // Compare histograms using correlation
        double correlation = Imgproc.compareHist(prevHist, currHist, Imgproc.HISTCMP_CORREL);
        Log.d("anik", "📊 Histogram correlation = " + correlation);

        mPrevGray = currGray.clone();
        return correlation >= HISTOGRAM_CORRELATION_THRESHOLD;
    }

    private int frameCount = 0;
    private int frameAfterToCheck = 2;
    private boolean lastStabilityResult = false;




    private int stableFrameCount = 0;
    private static final int REQUIRED_CONSECUTIVE_STABLE_FRAMES = 5;
    private boolean isBoxVisible = false;

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // Flip front camera (mirror)
        if (mIsFrontCamera) {
            Core.flip(mRgba, mRgba, 1);
        }

        Mat detectionInput = mRgba.clone();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.rotate(detectionInput, detectionInput, Core.ROTATE_90_CLOCKWISE);
        }


        frameCount++;

        // Run stability check every N frames
        if (frameCount % frameAfterToCheck == 0) {
            frameCount = 0;
            lastStabilityResult = isSceneStable(mRgba);

            if (lastStabilityResult) {
                stableFrameCount++;
                Log.d("anik02", "✅ Stable frame count: " + stableFrameCount);

                if (stableFrameCount >= REQUIRED_CONSECUTIVE_STABLE_FRAMES ) {

                    if(!isBoxVisible)
                    {
                        runOnUiThread(() -> {
                            mBoxOverlay.setVisibility(View.VISIBLE);
                            Log.d("anik02", "🎯 Box is now visible");
                        });
                        isBoxVisible = true;
                    }

                    if (isBoxVisible) {
                        // 1. Get the box in OpenCV coordinates
                        android.graphics.Rect screenBox = mBoxOverlay.getBoxRect();


// ✅ FIXED: use rotated size for ROI mapping
                        Rect roi = mapBoxRectToOpenCV(screenBox, detectionInput.size());
                        roi = adjustRectToBounds(roi, detectionInput.cols(), detectionInput.rows());

                        Mat faceRegionRgba = new Mat(detectionInput, roi);

// 👉 4. Convert to RGB
                        Mat faceRegionRgb = new Mat();
                        Imgproc.cvtColor(faceRegionRgba, faceRegionRgb, Imgproc.COLOR_RGBA2RGB);

// 5. Configure face detector for this region size
                        mFaceDetector.setInputSize(new Size(roi.width, roi.height));

// 6. Run detection
                        Mat faces = new Mat();
                        mFaceDetector.detect(faceRegionRgb, faces);

                        Log.d("anik03", "Face count in box = " + faces.rows());

// 7. Show green/red box based on face presence
                        boolean compliant = faces.rows() == 1;
                        runOnUiThread(() -> {
                            mBoxOverlay.setCompliant(compliant);
                        });

// 8. Release memory
                        faces.release();
                        faceRegionRgb.release();
                        faceRegionRgba.release();
                    }

                }

            } else {
                Log.d("anik01", "❌ Frame unstable — resetting stability count");
                stableFrameCount = 0;

                if (isBoxVisible) {
                    runOnUiThread(() -> {
                        mBoxOverlay.setVisibility(View.INVISIBLE);
                        Log.d("anik01", "🚫 Box is hidden due to instability");
                    });
                    isBoxVisible = false;
                }
            }
        }

        return mRgba;
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


/*

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        // Fix front camera orientation (vertical flip)
        if (mIsFrontCamera) {
            Core.flip(mRgba, mRgba, 1); // Horizontal flip (mirror)
        }

        Imgproc.cvtColor(mRgba, mRgb, Imgproc.COLOR_RGBA2RGB);

        Imgproc.resize(mRgb, mResized, FaceDetectionModel.DEFAULT_INPUT_SIZE);  // mInputSize = new Size(320, 320)

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.rotate(mResized, mResized, Core.ROTATE_90_CLOCKWISE);
        }

        android.graphics.Rect screenBox = mBoxOverlay.getBoxRect();
        android.graphics.Rect modelBox = mapBoxRectToResizedInput(screenBox, mRgba.size(), mResized.size()); // map screen to resized

        // Crop the region within the box from resized image
        Mat faceRegion = new Mat(mResized, new Rect(modelBox.left, modelBox.top,
                modelBox.width(), modelBox.height()));

        mFaceDetector.setInputSize(new Size(faceRegion.width(), faceRegion.height()));

        // Detect faces
        Mat faces = new Mat();

        Log.d(TAG, "Resized Image = [" + mResized + "]");

        mFaceDetector.detect(faceRegion, faces);

        Log.d(TAG, "Number of faces: " + faces.rows());

        boolean faceDetected = faces.rows() == 1;

// Save only once when face is detected for the first time
        if (faceDetected && !facePreviouslyDetected && !faceImageSaved) {
            saveFrame(mResized, "face_detected.jpg");
            faceImageSaved = true;
            noFaceImageSaved = false; // reset for next no-face condition
        }

// Save only once when face is lost
        if (!faceDetected ) {
            saveFrame(mResized, "no_face_detected.jpg");
            noFaceImageSaved = true;
            faceImageSaved = false; // reset for next face condition
        }

        facePreviouslyDetected = faceDetected;

        Log.d(TAG, "Faces matrix = [" + faces + "]");
        if (faces.rows() > 0){
            double[] x = faces.get(0, 0); // This should return 15 values: [x, y, w, h, eye1_x, eye1_y, ... score]
            double[] y = faces.get(0, 1);
            double[] w = faces.get(0, 2);
            double[] h = faces.get(0, 3);

            Log.d(TAG, "onCameraFrame() called with: inputFrame = [" + inputFrame + "]");
            Log.d(TAG, "Coordinates x = " + Arrays.toString(x) + " y = " + Arrays.toString(y) + " w " + Arrays.toString(w) + " h " + Arrays.toString(h));
            android.graphics.Rect boxRect = mBoxOverlay.getBoxRect();
            Log.d(TAG, "Box Coordinates x = " + boxRect.left + " y = " + boxRect.top + " w " + (boxRect.right - boxRect.left) + " h " + (boxRect.top - boxRect.bottom));

        }

//        mBoxOverlay.setCompliant(faces.rows() == 1);

        ComplianceResult state = faces.rows() == 0 ? ComplianceResult.NO_FACE :
                faces.rows() == 1 ? ComplianceResult.COMPLIED :
                        ComplianceResult.MULTIPLE_FACE;

        Log.d(TAG, "Face detected = [" + state + "]");

        switch (state) {
            case NO_FACE:
                handleNoFace();
                break;
            case COMPLIED:
                handleSingleFace(faces);
                break;
            case MULTIPLE_FACE:
                handleMultipleFaces();
                break;
        }

        if (faces.rows() > 0) {
            double[] x1 = faces.get(0,0);
            double[] y1 = faces.get(0,1);
            double[] w1 = faces.get(0,2);
            double[] h1 = faces.get(0,3);

            Log.d(TAG, "Drawing Bbox");

            Imgproc.rectangle(
                    mRgba,
                    new Point(x1[0], y1[0]),
                    new Point(x1[0] + w1[0], y1[0] + h1[0]),
                    new Scalar(0, 255, 0), // Green box
                    2
            );
        }

        faces.release();
        return mRgba;
    }
*/

    private void handleNoFace() {
        runOnUiThread(() -> {
            mStatusText.setText("No face detected - please position your face in the frame");
            mBoxOverlay.setCompliant(false);
            mCapture.setEnabled(false);
        });
    }

    private void handleSingleFace(Mat faces) {

        double[] x = faces.get(0, 0); // This should return 15 values: [x, y, w, h, eye1_x, eye1_y, ... score]
        double[] y = faces.get(0, 1);
        double[] w = faces.get(0, 2);
        double[] h = faces.get(0, 3);

        Rect faceRect = new Rect((int)x[0], (int)y[0], (int)w[0], (int)h[0]);

//        if (!isWithinComplianceBox(faceRect)) {
//            runOnUiThread(() -> {
//                mStatusText.setText("Center your face in the frame");
//                mBoxOverlay.setCompliant(false);
//                mCapture.setEnabled(false);
//            });
//            return;
//        }

        ICAOComplianceUtils.checkICAOCompliance(
                faces,
                new Size(mRgb.cols(), mRgb.rows()),
                (compliant, message) -> runOnUiThread(() -> {
                    mStatusText.setText(message);
                    mBoxOverlay.setCompliant(compliant);
                    mCapture.setEnabled(compliant);

                    if (compliant) {
                        Imgproc.rectangle(mRgb,
                                faceRect.tl(), faceRect.br(),
                                new Scalar(0, 255, 0), 3);
                    }
                })
        );
    }

    private void handleMultipleFaces() {
        runOnUiThread(() -> {
            mStatusText.setText("Multiple faces detected - only one person should be in frame");
            mBoxOverlay.setCompliant(false);
            mCapture.setEnabled(false);
        });
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
        if (boxRect == null || mRgba == null) return false;

        // Scale OpenCV face rect to view space
        float scaleX = (float) boxRect.width() / mRgba.cols();
        float scaleY = (float) boxRect.height() / mRgba.rows();

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
        if (mRgba != null && !mRgba.empty()) {
            try {
                Bitmap bmp = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
                org.opencv.android.Utils.matToBitmap(mRgba, bmp);
                mResultBmp = bmp;
                Toast.makeText(this, "Photo captured!", Toast.LENGTH_SHORT).show();
                // TODO: Process or save the captured image
            } catch (Exception e) {
                Log.e(TAG, "Error capturing image", e);
                Toast.makeText(this, "Error capturing image", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void saveFrame(Mat frame, String fileName) {
        try {
            Bitmap bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(frame, bmp);

            File path = getExternalFilesDir(null);
            File file = new File(path, fileName);
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Log.d(TAG, "Frame saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving frame", e);
        }
    }

}