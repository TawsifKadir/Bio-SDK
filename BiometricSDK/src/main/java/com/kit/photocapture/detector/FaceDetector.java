package com.kit.photocapture.detector;

import org.opencv.core.Mat;
import org.opencv.core.Size;

public interface FaceDetector {
    void loadDetector() throws Exception;
    void detect(Mat input, Mat output);
    void setInputSize(Size size);
}
