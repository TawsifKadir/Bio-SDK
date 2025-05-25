package com.kit.photocapture.util.faceDetectionUtils;


import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.Arrays;

public class FrameStabilityUtils {
    private static final String TAG = "FrameStabilityUtils";


    private static final double HISTOGRAM_CORRELATION_THRESHOLD = 0.92; // 1.0 = identical
    private static final int HIST_SIZE = 64; // Number of histogram bins
    private static final float[] HIST_RANGE = {0f, 256f}; // Grayscale range


    public static StabilityResult isSceneStable(
            Mat currRgba,
            Mat prevGray
    ) {
        Mat currGray = new Mat();
        Imgproc.cvtColor(currRgba, currGray, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(currGray, currGray, new Size(3, 3), 0);

        if (prevGray == null) {
            Log.d(TAG, "🔄 First frame, skipping stability check.");
            return new StabilityResult(false, currGray);
        }

        Mat currHist = new Mat();
        Imgproc.calcHist(Arrays.asList(currGray), new MatOfInt(0), new Mat(), currHist,
                new MatOfInt(HIST_SIZE), new MatOfFloat(HIST_RANGE));
        Core.normalize(currHist, currHist, 0, 1, Core.NORM_MINMAX);

        Mat prevHist = new Mat();
        Imgproc.calcHist(Arrays.asList(prevGray), new MatOfInt(0), new Mat(), prevHist,
                new MatOfInt(HIST_SIZE), new MatOfFloat(HIST_RANGE));
        Core.normalize(prevHist, prevHist, 0, 1, Core.NORM_MINMAX);

        double correlation = Imgproc.compareHist(prevHist, currHist, Imgproc.HISTCMP_CORREL);
        Log.d(TAG, "📊 Histogram correlation = " + correlation);

        boolean isStable = correlation >= HISTOGRAM_CORRELATION_THRESHOLD;
        return new StabilityResult(isStable, currGray);
    }

    public static class StabilityResult {
        public final boolean isStable;
        public final Mat updatedPreviousFrame;

        public StabilityResult(boolean isStable, Mat updatedPreviousFrame) {
            this.isStable = isStable;
            this.updatedPreviousFrame = updatedPreviousFrame;
        }
    }
}
