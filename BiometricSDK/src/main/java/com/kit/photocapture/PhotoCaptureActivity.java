package com.kit.photocapture;



import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Math;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.FaceDetectorYN;
import org.opencv.imgproc.Imgproc;
import org.opencv.osgi.OpenCVInterface;

import android.Manifest;
import android.app.Activity;
import android.app.AsyncNotedAppOp;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.camera2.CaptureResult;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.yalantis.ucrop.UCrop;
import com.yalantis.ucrop.UCropActivity;

import com.kit.biometricsdk.R;

public class PhotoCaptureActivity extends CameraActivity implements CvCameraViewListener2 , PictureDataCallback {

    private static final String    TAG  = "PhotoCaptureActivity";
    public static final String INTENT_IMAGE_PICKER_OPTION = "image_picker_option";
    public static final String INTENT_ASPECT_RATIO_X = "aspect_ratio_x";
    public static final String INTENT_ASPECT_RATIO_Y = "aspect_ratio_Y";
    public static final String INTENT_LOCK_ASPECT_RATIO = "lock_aspect_ratio";
    public static final String INTENT_IMAGE_COMPRESSION_QUALITY = "compression_quality";
    public static final String INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT = "set_bitmap_max_width_height";
    public static final String INTENT_BITMAP_MAX_WIDTH = "max_width";
    public static final String INTENT_BITMAP_MAX_HEIGHT = "max_height";

    private  float FACE_CONFIDENCE_THRESHHOLD = (float)0.5;

    private  int MAX_IMAGE_WIDTH = 240;
    private  int MAX_IMAGE_HEIGHT = 320;

    private  int ASPECT_RATIO_X = 9;

    private  int ASPECT_RATIO_Y = 16;

    private boolean LOCK_ASPECT_RATIO = false;
    private boolean SET_BITMAP_MAX_WIDTH_HEIGHT = true;
    private int IMAGE_COMPRESSION = 80;

    private static final Scalar    BOX_COLOR         = new Scalar(0, 255, 0);
    private static final Scalar    RIGHT_EYE_COLOR   = new Scalar(255, 0, 0);
    private static final Scalar    LEFT_EYE_COLOR    = new Scalar(0, 0, 255);
    private static final Scalar    NOSE_TIP_COLOR    = new Scalar(0, 255, 0);
    private static final Scalar    MOUTH_RIGHT_COLOR = new Scalar(255, 0, 255);
    private static final Scalar    MOUTH_LEFT_COLOR  = new Scalar(0, 255, 255);

    private Mat                    mRgba;
    private Mat                    mBgr;
    private Mat                    mBgrScaled;
    private Size                   mInputSize = null;

    private float                  mScale = 2.f;

    private CameraBridgeViewBase   mOpenCvCameraView;

    private MatOfByte              mModelBuffer;
    private MatOfByte              mConfigBuffer;

    private ImageButton mBack;
    private ImageButton mCapture;

    private ImageButton mSwitch;

    private FaceDetectorYN         mFaceDetector;
    private FaceDetectorYN         mComplianceFaceDetector;
    private Mat                    mFaces;

    private Uri mResultUri = null;
    private Bitmap mResultBmp = null;

    private int nowCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;

    public static boolean isDebug = false;

    private ComplianceResult mComplianceResult = null;

    public PhotoCaptureActivity() {
        if(PhotoCaptureActivity.isDebug)
            Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    if(PhotoCaptureActivity.isDebug)
                        Log.i(TAG, "OpenCV loaded successfully");

                    loadFaceDetector();

                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        boolean isError = false;
        Throwable errorObject = null;

        if(PhotoCaptureActivity.isDebug)
            Log.i(TAG, "called onCreate");

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null) {
            Toast.makeText(getApplicationContext(), "Null intent received", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            if (intent.hasExtra(INTENT_ASPECT_RATIO_X)) {
                ASPECT_RATIO_X = intent.getIntExtra(INTENT_ASPECT_RATIO_X, ASPECT_RATIO_X);
            }
            if (intent.hasExtra(INTENT_ASPECT_RATIO_Y)) {
                ASPECT_RATIO_Y = intent.getIntExtra(INTENT_ASPECT_RATIO_Y, ASPECT_RATIO_Y);
            }
            if (intent.hasExtra(INTENT_IMAGE_COMPRESSION_QUALITY)) {
                IMAGE_COMPRESSION = intent.getIntExtra(INTENT_IMAGE_COMPRESSION_QUALITY, IMAGE_COMPRESSION);
            }
            if (intent.hasExtra(INTENT_LOCK_ASPECT_RATIO)) {
                LOCK_ASPECT_RATIO = intent.getBooleanExtra(INTENT_LOCK_ASPECT_RATIO, false);
            }
            if (intent.hasExtra(INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT)) {
                SET_BITMAP_MAX_WIDTH_HEIGHT = intent.getBooleanExtra(INTENT_SET_BITMAP_MAX_WIDTH_HEIGHT, false);
            }
            if (intent.hasExtra(INTENT_BITMAP_MAX_WIDTH)) {
                MAX_IMAGE_WIDTH = intent.getIntExtra(INTENT_BITMAP_MAX_WIDTH, MAX_IMAGE_WIDTH);
            }
            if (intent.hasExtra(INTENT_BITMAP_MAX_HEIGHT)) {
                MAX_IMAGE_HEIGHT = intent.getIntExtra(INTENT_BITMAP_MAX_HEIGHT, MAX_IMAGE_HEIGHT);
            }

            if (!OpenCVLoader.initDebug()) {
                if (PhotoCaptureActivity.isDebug)
                    Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
                //// OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
            } else {
                if (PhotoCaptureActivity.isDebug)
                    Log.d(TAG, "OpenCV library found inside package. Using it!");
            }

            loadFaceDetector();

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            setContentView(R.layout.photo_capture_activity);

            mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.faisal_view);

            if (PhotoCaptureActivity.isDebug)
                Log.i(TAG, "OpenCVCameraView = " + mOpenCvCameraView);

            if (mOpenCvCameraView != null) {
                ((PhotoCaptureView) mOpenCvCameraView).setPictureDataCallback(this);
                mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
                mOpenCvCameraView.setCvCameraViewListener(this);
                mOpenCvCameraView.setCameraIndex(nowCameraIndex);
            }
            mBack = findViewById(R.id.backBtn);

            if (mBack != null) {
                mBack.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        prepareReturnData();
                        PhotoCaptureActivity.this.finish();
                    }
                });
            }

            mCapture = findViewById(R.id.captureBtn);

            if (mCapture != null) {
                mCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((PhotoCaptureView) mOpenCvCameraView).takePicture();
                    }
                });
            }

            mSwitch = findViewById(R.id.switchBtn);
            if (mSwitch != null) {
                mSwitch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        boolean isError = false;
                        Throwable errorObject = null;
                        if (mOpenCvCameraView != null) {
                            mOpenCvCameraView.disableView();
                            try {
                                if (nowCameraIndex == CameraBridgeViewBase.CAMERA_ID_BACK) {
                                    mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
                                    nowCameraIndex = CameraBridgeViewBase.CAMERA_ID_FRONT;
                                } else if (nowCameraIndex == CameraBridgeViewBase.CAMERA_ID_FRONT) {
                                    mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK);
                                    nowCameraIndex = CameraBridgeViewBase.CAMERA_ID_BACK;
                                }
                            } catch (Throwable t) {
                                isError = true;
                                errorObject = t;
                            } finally {

                                mOpenCvCameraView.enableView();

                                if (isError) {
                                    if (PhotoCaptureActivity.isDebug) {
                                        Log.e(TAG, "Error occurred while switching camera : " + errorObject.getMessage());
                                        errorObject.printStackTrace();
                                    }
                                    errorObject = null;
                                }

                            }
                        }
                    }
                });
            }

            mComplianceResult = ComplianceResult.NOT_INITIALIZED;
        }catch(Throwable t){
            isError = true;
            errorObject=t;
        }finally {
            if(isError){
                Log.e(TAG,errorObject.getMessage());
                errorObject.printStackTrace();
                errorObject=null;
            }
        }
        ///getPermissions();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        mComplianceResult = ComplianceResult.NOT_INITIALIZED;
    }

    @Override
    public void onResume()
    {
        super.onResume();
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.enableView();

        mComplianceResult = ComplianceResult.NOT_INITIALIZED;
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mBgr = new Mat();
        mBgrScaled = new Mat();
        mFaces = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mBgr.release();
        mBgrScaled.release();
        mFaces.release();
    }

    public void visualize(Mat rgba, Mat faces) {

        int thickness = 2;
        float[] faceData = new float[faces.cols() * faces.channels()];

        for (int i = 0; i < faces.rows(); i++)
        {
            faces.get(i, 0, faceData);

            if(PhotoCaptureActivity.isDebug) {
                Log.d(TAG, "Detected face (" + faceData[0] + ", " + faceData[1] + ", " +
                        faceData[2] + ", " + faceData[3] + ")");
                Log.d(TAG, "Detected face Score : " + faceData[14]);

            }

            if(faceData[14]<=FACE_CONFIDENCE_THRESHHOLD) continue;

            // Draw bounding box
            Imgproc.rectangle(rgba, new Rect(Math.round(mScale*faceData[0]), Math.round(mScale*faceData[1]),
                            Math.round(mScale*faceData[2]), Math.round(mScale*faceData[3])),
                    BOX_COLOR, thickness);
            // Draw landmarks
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[4]), Math.round(mScale*faceData[5])),
                    2, RIGHT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[6]), Math.round(mScale*faceData[7])),
                    2, LEFT_EYE_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[8]), Math.round(mScale*faceData[9])),
                    2, NOSE_TIP_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[10]), Math.round(mScale*faceData[11])),
                    2, MOUTH_RIGHT_COLOR, thickness);
            Imgproc.circle(rgba, new Point(Math.round(mScale*faceData[12]), Math.round(mScale*faceData[13])),
                    2, MOUTH_LEFT_COLOR, thickness);



        }
    }
    public synchronized ComplianceResult checkCompliance(Mat nowRgba) {

        Mat nowMBgr = new Mat();
        Mat nowMBgrScaled = new Mat();
        Mat nowFaces = new Mat();
        Size nowInputSize = new Size(Math.round(nowRgba.cols() / mScale), Math.round(nowRgba.rows() / mScale));
        boolean isError = false;
        Throwable errorObject = null;

        if(mFaceDetector!=null) {
            try {
                mComplianceFaceDetector.setInputSize(nowInputSize);

                Imgproc.cvtColor(nowRgba, nowMBgr, Imgproc.COLOR_RGBA2BGR);
                Imgproc.resize(nowMBgr, nowMBgrScaled, nowInputSize);

                int status = mComplianceFaceDetector.detect(nowMBgrScaled, nowFaces);

                if(PhotoCaptureActivity.isDebug) {
                    Log.d(TAG, "Detector returned status " + status);
                }

                if (nowFaces.rows() == 1) {

                    float[] faceData = new float[nowFaces.cols() * nowFaces.channels()];
                    nowFaces.get(0, 0, faceData);
                    if(faceData[14]<=FACE_CONFIDENCE_THRESHHOLD){
                        return ComplianceResult.LOW_QUALITY_FACE;
                    }else {
                        return ComplianceResult.COMPLIED;
                    }
                } else if (nowFaces.rows() > 1) {
                    return ComplianceResult.MULTIPLE_FACE;
                } else if (nowFaces.rows() <= 0) {
                    return ComplianceResult.NO_FACE;
                }

            }catch(Throwable t){
                isError=true;
                errorObject = t;
            }finally {
                if(isError){
                   //// Toast.makeText(PhotoCaptureActivity.this, "Error occured while checking compliance.", Toast.LENGTH_LONG).show();
                    errorObject.printStackTrace();
                }
                nowMBgr.release();
                nowMBgrScaled.release();
                nowFaces.release();
               /// mInputSize=null;
                errorObject=null;

            }

        }
        return ComplianceResult.UNKNOWN_ERROR;
    }
    public synchronized Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        boolean isError = false;
        Throwable errorObject = null;
        Mat rgbaFlipped = new Mat();

        try {

            mRgba = inputFrame.rgba();

//            if(nowCameraIndex!=CameraBridgeViewBase.CAMERA_ID_FRONT) {
//                Core.flip(inputFrame.rgba().t(), mRgba, 1);
//            }else{
//                Core.flip(inputFrame.rgba().t(), mRgba, 0);
//            }


            if (mInputSize == null) {
                mInputSize = new Size(Math.round(mRgba.cols() / mScale), Math.round(mRgba.rows() / mScale));
                mFaceDetector.setInputSize(mInputSize);
            }

            Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR);
            Imgproc.resize(mBgr, mBgrScaled, mInputSize);

            if (mFaceDetector != null) {
                int status = mFaceDetector.detect(mBgrScaled, mFaces);

                if(PhotoCaptureActivity.isDebug)
                    Log.d(TAG, "Detector returned status " + status);

                visualize(mRgba, mFaces);


            }

//            if(nowCameraIndex!=CameraBridgeViewBase.CAMERA_ID_FRONT) {
//                Core.flip(mRgba.t(), mRgba, 0);
//            }else{
//                mRgba = mRgba.t();
//            }

//            if(mFaces!=null && mFaces.rows()==1) {
//
//                float[] faceData = new float[mFaces.cols() * mFaces.channels()];
//                ///Drawing score
//                //Adding text to the image
//                Imgproc.putText(mRgba, "Score : " + Math.round(faceData[14]), new Point(mScale * faceData[0], (mScale * faceData[1]) - 20),
//                        Imgproc.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 255), 3);
//            }
        }catch(Throwable t){
            isError = true;
            errorObject = t;
        }finally {
            errorObject = null;
        }

//        if(nowCameraIndex==CameraBridgeViewBase.CAMERA_ID_FRONT) return rgbaFlipped;

        return mRgba;
    }



    @Override
    public void onPictureData(byte[] data) {
        if(PhotoCaptureActivity.isDebug)
            Log.d(TAG,"Picture Data Available");
        if(data!=null) {
            if(PhotoCaptureActivity.isDebug)
                Log.d(TAG, "Picture Data Size "+data.length);
            CaptureDataProcessor captureDataProcessor = new CaptureDataProcessor();
            captureDataProcessor.execute(data);
        }
    }


    public void startImageCropActivity(Bitmap image){
        Uri bmpUri = null;
        ///StringBuilder dstUriBuilder = null;
        Uri dstUri = null;
        ///File dstFile = null;
        boolean isError = false;
        Throwable errorObject = null;

        try {
            if (image != null) {
                bmpUri = Utility.getImageUri(PhotoCaptureActivity.this, image);
                Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                mResultBmp = Bitmap.createBitmap(image.getWidth(), image.getHeight(), conf);
                ////dstUri = Utility.getImageUri(PhotoCaptureActivity.this,mResultBmp);
                dstUri = Uri.fromFile(new File(getCacheDir(), Utility.queryName(getContentResolver(), bmpUri)));


                if(PhotoCaptureActivity.isDebug) {
                    Log.d(TAG, "Source Uri is : " + bmpUri.toString());
                    Log.d(TAG, "Destination Uri is : " + dstUri.toString());
                }

                UCrop.Options options = new UCrop.Options();
                options.setAllowedGestures(UCropActivity.SCALE, UCropActivity.ROTATE, UCropActivity.SCALE);
                options.setCompressionQuality(IMAGE_COMPRESSION);
                options.setFreeStyleCropEnabled(true);


                UCrop.of(bmpUri, dstUri)
                        .withAspectRatio(9-ASPECT_RATIO_X, ASPECT_RATIO_Y)
                        .withMaxResultSize(MAX_IMAGE_WIDTH,MAX_IMAGE_HEIGHT)
                        .withOptions(options)
                        .start(PhotoCaptureActivity.this);
            }
        }catch(Throwable t){
            isError = true;
            errorObject = t;
        }finally{
            if(isError){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PhotoCaptureActivity.this,"Error occurred while launching cropper ",Toast.LENGTH_LONG).show();
                    }
                });

                errorObject.printStackTrace();
            }
            bmpUri = null;
            dstUri = null;
            errorObject=null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(PhotoCaptureActivity.isDebug) {
            Log.d(TAG, ">>>>>Got Activity Result.");
            Log.d(TAG, ">>>>>Request Code : " + requestCode);
            Log.d(TAG, ">>>>>Result Code : " + resultCode);
        }

        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            mResultUri = UCrop.getOutput(data);
            prepareReturnData();
            PhotoCaptureActivity.this.finish();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            if(cropError!=null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(PhotoCaptureActivity.this, "Error occured " + cropError.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

                cropError.printStackTrace();
            }
            ((PhotoCaptureView)mOpenCvCameraView).startPreview();
        }
    }

    public void getPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{Manifest.permission.CAMERA},101);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(grantResults.length>0 && grantResults[0]!=PackageManager.PERMISSION_GRANTED){
            getPermissions();
        }
    }


    public void prepareReturnData(){
        Intent data = new Intent();
        try{
            if(mResultUri!=null) {
                data.putExtra("IMAGE_URI", mResultUri.toString());
                setResult(Activity.RESULT_OK, data);
            }else {
                setResult(Activity.RESULT_CANCELED, data);
            }

        }catch(Throwable t){
            setResult(Activity.RESULT_CANCELED,data);
            t.printStackTrace();
        }
    }

    private class CaptureDataProcessor extends AsyncTask<byte[],Object,Object>{

        @Override
        protected Object doInBackground(byte[]... bytes) {

            Mat nowImage = new Mat();
            Mat flippedImage = new Mat();
            Mat rotatedImage = new Mat();
            Bitmap flippedBmp = null;
            boolean isError = false;
            Throwable errorObject = null;
            byte[] data = bytes[0];

            if(data==null){
                return null;
            }

            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inMutable = true;
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, options);

                Utils.bitmapToMat(bmp, nowImage, false);

                if(nowCameraIndex!=CameraBridgeViewBase.CAMERA_ID_FRONT) {
                    Core.rotate(nowImage, flippedImage, Core.ROTATE_90_CLOCKWISE);
                }else{
                    Core.rotate(nowImage, rotatedImage, Core.ROTATE_90_COUNTERCLOCKWISE);
                    Core.flip(rotatedImage, flippedImage, 1);
                }


                ComplianceResult nowResult = checkCompliance(flippedImage);


                if(nowResult==null){
                    nowResult = ComplianceResult.UNKNOWN_ERROR;
                }

                if (nowResult != ComplianceResult.COMPLIED) {
                    mComplianceResult = nowResult;

                   /* Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                    flippedBmp = Bitmap.createBitmap(flippedImage.cols(), flippedImage.rows(), conf);
                    Utils.matToBitmap(flippedImage, flippedBmp);
                    startImageCropActivity(flippedBmp);
*/
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PhotoCaptureActivity.this, mComplianceResult.getComplianceTxt(), Toast.LENGTH_LONG).show();
                        }
                    });

                    ((PhotoCaptureView)mOpenCvCameraView).startPreview();
                } else {
                    Bitmap.Config conf = Bitmap.Config.ARGB_8888; // see other conf types
                    flippedBmp = Bitmap.createBitmap(flippedImage.cols(), flippedImage.rows(), conf);
                    Utils.matToBitmap(flippedImage, flippedBmp);
                    startImageCropActivity(flippedBmp);
                }
            }catch (Throwable t){
                isError = true;
                errorObject = t;
            }finally{
                if(isError){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(PhotoCaptureActivity.this, "Error occured while capturing photo.", Toast.LENGTH_LONG).show();
                        }
                    });

                    errorObject.printStackTrace();
                    ((PhotoCaptureView)mOpenCvCameraView).startPreview();
                }
                nowImage.release();
                flippedImage.release();
                rotatedImage.release();
                errorObject=null;
            }
            return null;
        }
    }

    public void loadFaceDetector(){
        boolean isError = false;
        Throwable errorObject = null;
        try {
            byte[] nowBuffer = CascadeLoader.getInstance(
                    PhotoCaptureActivity.this).loadModelBuffer(
                    R.raw.face_detection_yunet_2023mar);

            if (nowBuffer == null) {
                if(PhotoCaptureActivity.isDebug)
                    Log.e(TAG, "Could not load model file");
                mFaceDetector = null;
            } else {
                mModelBuffer = new MatOfByte(nowBuffer);
                mConfigBuffer = new MatOfByte();

                mFaceDetector = FaceDetectorYN.create("onnx", mModelBuffer, mConfigBuffer, new Size(320, 320));
                mComplianceFaceDetector = FaceDetectorYN.create("onnx", mModelBuffer, mConfigBuffer, new Size(320, 320));

                if (mFaceDetector == null) {
                    if(PhotoCaptureActivity.isDebug)
                        Log.e(TAG, "Failed to create FaceDetectorYN!");
                    (Toast.makeText(PhotoCaptureActivity.this, "Failed to create FaceDetectorYN!", Toast.LENGTH_LONG)).show();
                } else if(PhotoCaptureActivity.isDebug)
                    Log.i(TAG, "FaceDetectorYN initialized successfully!");

            }
            CascadeLoader.getInstance(PhotoCaptureActivity.this).deleteModelDir();
        }catch(Throwable t){
            isError = true;
            errorObject = t;
        }finally {
            if(isError){
                (Toast.makeText(PhotoCaptureActivity.this, "Error occurred while loading face detector "+errorObject.getMessage(), Toast.LENGTH_LONG)).show();
                errorObject.printStackTrace();
            }
            errorObject = null;
        }
    }


}
