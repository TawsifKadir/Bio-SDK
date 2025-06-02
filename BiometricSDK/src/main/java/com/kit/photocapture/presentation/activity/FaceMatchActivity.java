package com.kit.photocapture.presentation.activity;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.kit.biometricsdk.R;
import com.kit.photocapture.detector.YunetFaceDetectionImpl;
import com.kit.photocapture.model.detector.FaceDetectionModel;
import com.kit.photocapture.recognizer.FaceRecognizer;
import com.kit.photocapture.recognizer.SFaceRecognitionModelImpl;
import com.kit.photocapture.test.FaceComparisionTest;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

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
        bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.cr7);

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
            // Save bitmaps to internal files so FaceComparisionTest can read them
            File img1 = saveBitmapToTempFile(bitmap1, "face1.jpg");
            File img2 = saveBitmapToTempFile(bitmap2, "face2.jpg");


            String img1Base64 = imageFileToBase64(img1);
            Log.d("ImageBase64", "img1 (Base64): " + img1Base64);


            String img2Base64 = imageFileToBase64(img2);
            Log.d("ImageBase64", "img2 (Base64): " + img2Base64);



            // Load them into OpenCV Mats
            Mat mat1 = Imgcodecs.imread(img1.getAbsolutePath());
            Mat mat2 = Imgcodecs.imread(img2.getAbsolutePath());

            // Extend FaceComparisionTest to add a new compare method that accepts Mat directly
            FaceComparisionTest.compareTwoFaceMats(this, mat1, mat2, "Live Capture");

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("compareFaces", "Failed to compare", e);
        }
    }

    private String imageFileToBase64(File imageFile) {
        try {
            FileInputStream fis = new FileInputStream(imageFile);
            byte[] bytes = new byte[(int) imageFile.length()];
            fis.read(bytes);
            fis.close();

            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            Log.e("Base64Error", "Failed to convert image to Base64", e);
            return null;
        }
    }

    private File saveBitmapToTempFile(Bitmap bitmap, String filename) throws IOException {
        File file = new File(getCacheDir(), filename);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
        return file;
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
