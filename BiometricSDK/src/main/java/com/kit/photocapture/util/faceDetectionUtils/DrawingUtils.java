package com.kit.photocapture.util.faceDetectionUtils;


import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

public class DrawingUtils {

    public static void drawLandmark(Mat frame, Point point, Scalar color) {
        org.opencv.imgproc.Imgproc.circle(frame, point, 5, color, -1);
    }

    public static void drawFacialKeypoints(Mat faces, Rect roi, Mat rotatedInput, Mat originalFrame, int rotationFlag, boolean isFrontCamera) {
        if (faces.rows() < 1 || faces.cols() < 14) return;

        double[] row = new double[15];
        for (int i = 0; i < 15; i++) {
            double[] value = faces.get(0, i);
            row[i] = (value != null && value.length > 0) ? value[0] : 0.0;
        }

        Point[] rawPoints = new Point[] {
                new Point(row[4] + roi.x, row[5] + roi.y),
                new Point(row[6] + roi.x, row[7] + roi.y),
                new Point(row[8] + roi.x, row[9] + roi.y),
                new Point(row[10] + roi.x, row[11] + roi.y),
                new Point(row[12] + roi.x, row[13] + roi.y)
        };

        Scalar[] colors = new Scalar[] {
                new Scalar(255, 0, 0),     // 🔵 Left Eye
                new Scalar(255, 0, 0),     // 🔵 Right Eye
                new Scalar(0, 255, 255),   // 🟡 Nose
                new Scalar(0, 0, 255),     // 🔴 Mouth Left
                new Scalar(0, 0, 255)      // 🔴 Mouth Right
        };

        for (int i = 0; i < rawPoints.length; i++) {
            Point corrected = RotationUtils.rotatePointBack(rawPoints[i], rotatedInput.size(), originalFrame.size(), rotationFlag, isFrontCamera);
            drawLandmark(originalFrame, corrected, colors[i]);
        }
    }
}
