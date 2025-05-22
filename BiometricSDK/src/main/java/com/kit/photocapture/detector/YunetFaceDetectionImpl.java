package com.kit.photocapture.detector;

import android.content.Context;

import com.kit.photocapture.model.detector.FaceDetectionModel;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.objdetect.FaceDetectorYN;

public class YunetFaceDetectionImpl implements FaceDetector{

    private Context context;
    private FaceDetectorYN mFaceDetector;
    private FaceDetectionModel model;

    public YunetFaceDetectionImpl(Context context){
        this.context = context;
        this.model = new FaceDetectionModel(context);
        this.mFaceDetector = null;
    }
    public YunetFaceDetectionImpl(Context context, int width, int height){
        this.context = context;
        this.model = new FaceDetectionModel(context, width, height);
        this.mFaceDetector = null;
    }

    public YunetFaceDetectionImpl(Context context, int width, int height, double aspectRatio){
        this.context = context;
        this.model = new FaceDetectionModel(context, width, height, aspectRatio);
        this.mFaceDetector = null;
    }

    public YunetFaceDetectionImpl(Context context, MatOfByte config, int width, int height){
        this.context = context;
        this.model = new FaceDetectionModel(context, config, width, height);
        this.mFaceDetector = null;
    }

    public YunetFaceDetectionImpl(Context context, MatOfByte config, int width, int height, double aspectRatio){
        this.context = context;
        this.model = new FaceDetectionModel(context, config, width, height, aspectRatio);
        this.mFaceDetector = null;
    }
    @Override
    public void loadDetector() throws Exception{
        if (model == null){
            throw new Exception("Model not initialized");
        }
        mFaceDetector = model.getDetector();
    }

    @Override
    public void detect(Mat input, Mat output) {
        mFaceDetector.detect(input, output);
    }

    @Override
    public void setInputSize(Size size) {
        mFaceDetector.setInputSize(size);
    }
}
