package com.kit.photocapture.util.faceDetectionUtils;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class FaceComplianceUtils {


    private static final double MIN_FACE_AREA_RATIO = 0.10;

    private static final double MAX_FACE_AREA_RATIO = 0.40;

    private static final double MIN_FACE_SCORE = 0.90;
    private static final double PADDING_LEFT_RIGHT_RATIO = 0.05;
    private static final double PADDING_TOP_RATIO = 0.17;
    private static final double PADDING_BOTTOM_RATIO = 0.05;
    public static ComplianceResult isFaceCompliant(Mat faces, Rect roi) {

        if (faces.rows() != 1 || faces.cols() < 15) {
            String message = "Invalid face matrix";
            Log.d("isFaceCompliant", "❌ " + message);
            return new ComplianceResult(false, message);
        }



        double[] row = new double[15];
        for (int i = 0; i < 15; i++) {
            double[] value = faces.get(0, i);
            row[i] = (value != null && value.length > 0) ? value[0] : 0.0;
        }

        double x = row[0];
        double y = row[1];
        double w = row[2];
        double h = row[3];
        double score = row[14];

        double faceArea = w * h;
        double roiArea = roi.width * roi.height;
        double ratio = faceArea / roiArea;

        double paddingX = roi.width *PADDING_LEFT_RIGHT_RATIO;
        double paddingTop = roi.height * PADDING_TOP_RATIO;
        double paddingBottom = roi.height * PADDING_BOTTOM_RATIO;

        double innerLeft = paddingX;
        double innerTop = paddingTop;
        double innerRight = roi.width - paddingX;
        double innerBottom = roi.height - paddingBottom;

        Log.d("isFaceCompliant", String.format("x=%.2f y=%.2f w=%.2f h=%.2f score=%.3f areaRatio=%.3f", x, y, w, h, score, ratio));
        Log.d("isFaceCompliant", String.format("Inner box: left=%.2f top=%.2f right=%.2f bottom=%.2f", innerLeft, innerTop, innerRight, innerBottom));

        Log.d("isFaceCompliant", String.format("x=%.2f y=%.2f w=%.2f h=%.2f score=%.3f areaRatio=%.3f", x, y, w, h, score, ratio));

        if (ratio < MIN_FACE_AREA_RATIO) {
            return new ComplianceResult(false, "Face is too small. Move closer.");
        }
        if (ratio > MAX_FACE_AREA_RATIO) {
            return new ComplianceResult(false, "Face is too large. Move back slightly.");
        }

        if (x <= innerLeft) {
            return new ComplianceResult(false, "Face too close to the left edge.");
        }
        if (y <= innerTop) {
            return new ComplianceResult(false, "Face too close to the top edge.");
        }
        if ((x + w) >= innerRight) {
            return new ComplianceResult(false, "Face too close to the right edge.");
        }
        if ((y + h) >= innerBottom) {
            return new ComplianceResult(false, "Face too close to the bottom edge.");
        }
        if (score < MIN_FACE_SCORE) {
            return new ComplianceResult(false, "Detection confidence too low.");
        }

        return new ComplianceResult(true, "Face is compliant.");

    }
}
