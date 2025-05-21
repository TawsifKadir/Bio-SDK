package com.kit.photocapture.detector;

import org.opencv.core.Mat;

public interface FaceDetector {
    void loadDetector() throws Exception;
    void detect(Mat input, Mat output);
}
