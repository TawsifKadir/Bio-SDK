package com.kit.photocapture.util.faceDetectionUtils;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class FaceComplianceUtils {

    public static boolean isFaceCompliant(Mat faces, Rect roi) {
        if (faces.rows() != 1 || faces.cols() < 15) {
            Log.d("isFaceCompliant", "❌ Invalid face matrix: rows=" + faces.rows() + ", cols=" + faces.cols());
            return false;
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

        double paddingX = roi.width * 0.05;
        double paddingTop = roi.height * 0.17;
        double paddingBottom = roi.height * 0.05;

        double innerLeft = paddingX;
        double innerTop = paddingTop;
        double innerRight = roi.width - paddingX;
        double innerBottom = roi.height - paddingBottom;

        Log.d("isFaceCompliant", String.format("x=%.2f y=%.2f w=%.2f h=%.2f score=%.3f areaRatio=%.3f", x, y, w, h, score, ratio));
        Log.d("isFaceCompliant", String.format("Inner box: left=%.2f top=%.2f right=%.2f bottom=%.2f", innerLeft, innerTop, innerRight, innerBottom));

        boolean isRatioOK = ratio >= 0.25;
        boolean isLeftOK = x > innerLeft;
        boolean isTopOK = y > innerTop;
        boolean isRightOK = (x + w) < innerRight;
        boolean isBottomOK = (y + h) < innerBottom;
        boolean isScoreOK = score >= 0.90;

        if (!isRatioOK) Log.d("isFaceCompliant", "❌ Rejected: face area ratio too low (" + ratio + " < 0.25)");
        if (!isLeftOK) Log.d("isFaceCompliant", "❌ Rejected: face too close to left edge (x=" + x + ", innerLeft=" + innerLeft + ")");
        if (!isTopOK) Log.d("isFaceCompliant", "❌ Rejected: face too close to top edge (y=" + y + ", innerTop=" + innerTop + ")");
        if (!isRightOK) Log.d("isFaceCompliant", "❌ Rejected: face too close to right edge (x+w=" + (x + w) + ", innerRight=" + innerRight + ")");
        if (!isBottomOK) Log.d("isFaceCompliant", "❌ Rejected: face too close to bottom edge (y+h=" + (y + h) + ", innerBottom=" + innerBottom + ")");
        if (!isScoreOK) Log.d("isFaceCompliant", "❌ Rejected: face score too low (" + score + " < 0.91)");

        boolean result = isRatioOK && isLeftOK && isTopOK && isRightOK && isBottomOK && isScoreOK;

        Log.d("isFaceCompliant", result ? "✅ Compliant" : "❌ Not compliant");
        return result;
    }
}
