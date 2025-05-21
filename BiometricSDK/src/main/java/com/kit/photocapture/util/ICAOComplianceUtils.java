package com.kit.photocapture.util;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;

public class ICAOComplianceUtils {
    public interface ComplianceCallback {
        void onComplianceCheck(boolean isCompliant, String message);
    }

    // ICAO Compliance Constants
    private static final float MIN_FACE_SCORE = 0.7f;
    private static final float MIN_FACE_WIDTH_RATIO = 0.15f;
    private static final float MAX_FACE_WIDTH_RATIO = 0.35f;
    private static final float MIN_EYE_DISTANCE_RATIO = 0.15f;
    private static final float MAX_HEAD_TILT = 5.0f;
    private static final float MIN_EYE_MOUTH_RATIO = 1.4f;
    private static final float MAX_EYE_MOUTH_RATIO = 1.8f;

    public static void checkICAOCompliance(Mat face, Size frameSize, ComplianceCallback callback) {
        if (face == null || face.rows() == 0) {
            callback.onComplianceCheck(false, "No face detected");
            return;
        }
        double[] x = face.get(0, 0); // This should return 15 values: [x, y, w, h, eye1_x, eye1_y, ... score]
        double[] y = face.get(0, 1);
        double[] w = face.get(0, 2);
        double[] h = face.get(0, 3);

        Rect faceRect = new Rect((int)x[0], (int)y[0], (int)w[0], (int)h[0]);

        float confidence = (float)face.get(0,14)[0];
        Point[] landmarks = extractLandmarks(face, frameSize);

        // 2. Check confidence score
        if (confidence < MIN_FACE_SCORE) {
            callback.onComplianceCheck(false, "Low detection confidence");
            return;
        }

        // 3. Check face size
        float faceWidthRatio = (float)faceRect.width / (float)frameSize.width;
        if (faceWidthRatio < MIN_FACE_WIDTH_RATIO) {
            callback.onComplianceCheck(false, "Move closer to camera");
            return;
        }
        if (faceWidthRatio > MAX_FACE_WIDTH_RATIO) {
            callback.onComplianceCheck(false, "Move further from camera");
            return;
        }

        // 4. Check facial features
        String featureMessage = checkFacialFeatures(faceRect, landmarks);
        if (featureMessage != null) {
            callback.onComplianceCheck(false, featureMessage);
            return;
        }

        callback.onComplianceCheck(true, "Face meets ICAO standards");
    }

    private static Point[] extractLandmarks(Mat face, Size frameSize) {
        return new Point[] {
                new Point(face.get(0,4)[0] * frameSize.width, face.get(0,5)[0] * frameSize.height),  // right eye
                new Point(face.get(0,6)[0] * frameSize.width, face.get(0,7)[0] * frameSize.height),  // left eye
                new Point(face.get(0,8)[0] * frameSize.width, face.get(0,9)[0] * frameSize.height),  // nose
                new Point(face.get(0,10)[0] * frameSize.width, face.get(0,11)[0] * frameSize.height), // mouth right
                new Point(face.get(0,12)[0] * frameSize.width, face.get(0,13)[0] * frameSize.height)  // mouth left
        };
    }

    private static String checkFacialFeatures(Rect faceRect, Point[] landmarks) {
        // Eye distance check
        double eyeDistance = Math.sqrt(Math.pow(landmarks[0].x - landmarks[1].x, 2) +
                Math.pow(landmarks[0].y - landmarks[1].y, 2));
        if (eyeDistance < faceRect.width * MIN_EYE_DISTANCE_RATIO) {
            return "Face too far or eyes not visible";
        }

        // Head tilt calculation
        double deltaX = landmarks[1].x - landmarks[0].x;
        double deltaY = landmarks[1].y - landmarks[0].y;
        double angle = Math.toDegrees(Math.atan2(deltaY, deltaX));

        if (angle < -MAX_HEAD_TILT) {
            return "Tilt your head to the right";
        }
        if (angle > MAX_HEAD_TILT) {
            return "Tilt your head to the left";
        }

        // Eye-mouth proportion check
        double eyeLineY = (landmarks[0].y + landmarks[1].y) / 2;
        double mouthLineY = (landmarks[3].y + landmarks[4].y) / 2;
        double noseY = landmarks[2].y;

        if (!(eyeLineY < noseY && noseY < mouthLineY)) {
            return "Keep your head straight";
        }

        double eyeMouthRatio = (mouthLineY - eyeLineY) / (noseY - eyeLineY);
        if (eyeMouthRatio < MIN_EYE_MOUTH_RATIO || eyeMouthRatio > MAX_EYE_MOUTH_RATIO) {
            return "Adjust your head position";
        }

        return null;
    }
}
