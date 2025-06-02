package com.kit.photocapture.presentation.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.kit.biometricsdk.R;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceMatchActivity extends Activity {

    private static final Logger log = LoggerFactory.getLogger(FaceMatchActivity.class);
    private ImageView image1View, image2View;
    private Button matchButton;

    private Bitmap bitmap1, bitmap2;
    private static final float MATCH_THRESHOLD = 0.6f;
    private static final String TAG = "FaceMatchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face_match);

        image1View = findViewById(R.id.image1);
        image2View = findViewById(R.id.image2);
        matchButton = findViewById(R.id.btn_match);

        bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.akon);
        bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.bolt);

        image1View.setImageBitmap(bitmap1);
        image2View.setImageBitmap(bitmap2);

        matchButton.setOnClickListener(v -> compareFaces());
    }

    private void compareFaces() {
        if (!OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV failed to load", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Convert Bitmaps to Mat
            Mat mat1 = new Mat();
            Mat mat2 = new Mat();
            Utils.bitmapToMat(bitmap1, mat1);
            Utils.bitmapToMat(bitmap2, mat2);

            mat1 = convertToRGB(mat1);
            mat2 = convertToRGB(mat2);

            // Resize both to 320x320
            Size modelInputSize = FaceDetectionModel.DEFAULT_INPUT_SIZE;
            Imgproc.resize(mat1, mat1, modelInputSize);
            Imgproc.resize(mat2, mat2, modelInputSize);

            // Face Detector setup
            YunetFaceDetectionImpl detector = new YunetFaceDetectionImpl(this);
            detector.loadDetector();
            detector.setInputSize(modelInputSize);

            Mat faces1 = new Mat();
            Mat faces2 = new Mat();


            // Do NOT resize the image.
            // Instead, use its actual size as the model input
            Size inputSize1 = new Size(mat1.cols(), mat1.rows());
            Size inputSize2 = new Size(mat2.cols(), mat2.rows());

            // Set size individually before detection
            detector.setInputSize(inputSize1);
            detector.detect(mat1, faces1);

            detector.setInputSize(inputSize2);
            detector.detect(mat2, faces2);

            if (faces1.rows() == 0 && faces2.rows() == 0) {
                Toast.makeText(this, "No face detected in  both images", Toast.LENGTH_LONG).show();
                return;
            } else if (faces1.rows() == 0 ) {
                Toast.makeText(this, "No face detected in faces1 images", Toast.LENGTH_LONG).show();
                return;
            }
            else if (faces2.rows() == 0) {
                Toast.makeText(this, "No face detected in faces2 images", Toast.LENGTH_LONG).show();
                return;
            }

//            Log.d(TAG, "compareFaces() called  "+ "  " + faces1.rows() + "  " +faces2.rows()  );



            // Extract box info
            float[] faceData1 = new float[14];
            float[] faceData2 = new float[14];
            faces1.get(0, 0, faceData1);
            faces2.get(0, 0, faceData2);

            Mat faceBox1 = new Mat(1, 4, CvType.CV_32FC1);
            faceBox1.put(0, 0, faceData1[0], faceData1[1], faceData1[2], faceData1[3]);

            Mat faceBox2 = new Mat(1, 4, CvType.CV_32FC1);
            faceBox2.put(0, 0, faceData2[0], faceData2[1], faceData2[2], faceData2[3]);

            // Recognizer pipeline
            FaceRecognizer recognizer = new SFaceRecognitionModelImpl(this);
            recognizer.loadRecognizer();

            Mat aligned1 = new Mat();
            Mat aligned2 = new Mat();
            recognizer.alignCrop(mat1, faceBox1, aligned1);
            recognizer.alignCrop(mat2, faceBox2, aligned2);


            Log.d("Debug", "Aligned1 size: " + aligned1.size());
            Log.d("Debug", "Aligned2 size: " + aligned2.size());
            Log.d("Debug", "Aligned1 pixel: " + aligned1.get(0, 0)[0]);
            Log.d("Debug", "Aligned2 pixel: " + aligned2.get(0, 0)[0]);
            Log.d("FaceBox", "Face1 Box: " + faceBox1.dump());
            Log.d("FaceBox", "Face2 Box: " + faceBox2.dump());




            Mat feature1 = new Mat();
            Mat feature2 = new Mat();
            recognizer.extractFeature(aligned1, feature1);
            feature1 = feature1.clone();

            recognizer.extractFeature(aligned2, feature2);
            feature2 = feature2.clone();



            logFeatureVector(feature1, "Feature1");
            logFeatureVector(feature2, "Feature2");


            Log.d(TAG, "compareFaces() called: " +  feature1.size());

            double similarity = recognizer.compareFeatures(feature1, feature2);
            boolean isMatch = recognizer.isMatch(feature1, feature2, MATCH_THRESHOLD);

            String result = String.format("Similarity: %.2f%%\nMatch: %s",
                    similarity * 100, isMatch ? "YES ✅" : "NO ❌");

            Toast.makeText(this, result, Toast.LENGTH_LONG).show();
            Log.i(TAG, result);

        } catch (Exception e) {
            Log.e(TAG, "Face match error", e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Mat convertToRGB(Mat input) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(input, rgb, Imgproc.COLOR_RGBA2RGB);
        return rgb;
    }
    private void logFeatureVector(Mat feature, String tag) {
        if (feature.rows() == 1 && feature.cols() > 0 && feature.type() == CvType.CV_32FC1) {
            StringBuilder sb = new StringBuilder();
            sb.append(tag).append(" [");
            for (int i = 0; i < feature.cols(); i++) {
                float[] val = new float[1];
                feature.get(0, i, val);
                sb.append(String.format("%.6f", val[0]));
                if (i < feature.cols() - 1) sb.append(", ");
            }
            sb.append("]");
            Log.d("FeatureVector", sb.toString());
        } else {
            Log.w("FeatureVector", tag + " is not a valid 1xN float matrix");
        }
    }


}
