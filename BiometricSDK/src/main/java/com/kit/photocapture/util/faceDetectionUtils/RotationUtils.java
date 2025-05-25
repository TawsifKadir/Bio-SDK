package com.kit.photocapture.util.faceDetectionUtils;


import org.opencv.core.Point;
import org.opencv.core.Size;

public class RotationUtils {
    public static Point rotatePointBack(Point p, Size rotatedSize, Size originalSize, int rotationFlag, boolean isFrontCamera) {
        Point unrotated;
        switch (rotationFlag) {
            case org.opencv.core.Core.ROTATE_90_CLOCKWISE:
                unrotated = new Point(rotatedSize.height - p.y, p.x);
                break;
            case org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE:
                unrotated = new Point(p.y, rotatedSize.width - p.x);
                break;
            case org.opencv.core.Core.ROTATE_180:
                unrotated = new Point(rotatedSize.width - p.x, rotatedSize.height - p.y);
                break;
            default:
                unrotated = p;
        }

        if (isFrontCamera) {
            unrotated.x = originalSize.width - unrotated.x;
        }

        unrotated.y = originalSize.height - unrotated.y;

        return unrotated;
    }
}
