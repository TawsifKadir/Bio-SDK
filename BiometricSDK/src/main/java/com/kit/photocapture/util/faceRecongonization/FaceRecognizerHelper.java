package com.kit.photocapture.util.faceRecongonization;



import android.content.Context;
import android.graphics.Bitmap;

import com.kit.photocapture.detector.FaceDetector;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class FaceRecognizerHelper {

    private final FaceRecognizer recognizer;
    private final FaceDetector detector;
    public FaceRecognizerHelper(Context context) throws Exception {
        recognizer = new SFaceRecognitionModelImpl(context);
        recognizer.loadRecognizer();

        detector = new YunetFaceDetectionImpl(context);
        detector.loadDetector();

    }


    public Mat getFaceFeatureMatrix(Mat image, Mat faceCorrdinatesForImage) throws Exception {


        FaceRecognizer recognizer = this.recognizer;
        recognizer.loadRecognizer();

        Mat aligned1 = new Mat();

        recognizer.alignCrop(image, faceCorrdinatesForImage.row(0), aligned1);

        Mat feature1 = new Mat();
        recognizer.extractFeature(aligned1, feature1);
        return feature1.clone();

    }
    public boolean  isFaceMatched(Mat feature1, Mat feature2,float threshold) {
      return  recognizer.isMatch(feature1, feature2, threshold);

    }
    public double  compareFeatures(Mat feature1, Mat feature2) {
        return  recognizer.compareFeatures(feature1, feature2);

    }

    public int numOfFaces(Mat detectedModelSuitedFrame) {

        return detectedModelSuitedFrame.rows();
    }




    public  Mat makeFrameSuitToTheModel(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2RGB);
        Imgproc.resize(mat, mat, FaceDetectionModel.DEFAULT_INPUT_SIZE);
        return mat;
    }

    public Mat detectFace(Mat image) throws Exception {

        detector.setInputSize(FaceDetectionModel.DEFAULT_INPUT_SIZE);
        Mat faces = new Mat();
        detector.detect(image, faces);
        return faces;
    }

}
