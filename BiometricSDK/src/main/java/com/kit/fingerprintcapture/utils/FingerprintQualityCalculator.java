package com.kit.fingerprintcapture.utils;

import android.graphics.Bitmap;
import android.graphics.Color;

public class FingerprintQualityCalculator {

    // Constants to adjust the weight of contrast and coverage in the final score
    private static final double CONTRAST_WEIGHT = 0.3;
    private static final double COVERAGE_WEIGHT = 0.9;

    // Define the ranges for contrast and coverage normalization
    private static final double MIN_CONTRAST = 0.0;
    private static final double MAX_CONTRAST = 255.0;  // Assuming 8-bit grayscale image
    private static final double MIN_COVERAGE = 0.0;
    private static final double MAX_COVERAGE = 60.0;  // Coverage is given as a percentage

    // Calculate the fingerprint quality from an ARGB_8888 image
    public static double calculateFingerprintQualityFromImage(Bitmap fingerImage) {
        return calculateFingerprintQuality(calculateContrast(fingerImage), calculateCoverage(fingerImage));
    }

    /**
     * Calculates the fingerprint quality score based on contrast and coverage.
     * @param contrast The contrast of the fingerprint image (0-255).
     * @param coverage The coverage of the fingerprint (percentage of dark pixels).
     * @return A fingerprint quality score from 0.0 (poor) to 1.0 (excellent).
     */
    public static double calculateFingerprintQuality(double contrast, double coverage) {
        // Normalize the contrast and coverage values to be between 0 and 1
        double normalizedContrast = normalize(contrast, MIN_CONTRAST, MAX_CONTRAST);
        double normalizedCoverage = normalize(coverage, MIN_COVERAGE, MAX_COVERAGE);

        // Calculate the weighted sum of contrast and coverage
        double qualityScore = (normalizedContrast * CONTRAST_WEIGHT) + (normalizedCoverage * COVERAGE_WEIGHT);

        // Ensure the score is between 0 and 1
        return clamp(qualityScore, 0.0, 1.0);
    }

    /**
     * Normalize a value to be between 0 and 1 based on its range.
     */
    private static double normalize(double value, double min, double max) {
        return (value - min) / (max - min);
    }

    /**
     * Clamp a value to be between min and max.
     */
    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // Calculate the coverage (percentage of dark pixels) for an ARGB_8888 image
    public static double calculateCoverage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;

        int darkPixelCount = 0;

        // Define a threshold for dark pixels (0-255 grayscale)
        int darkPixelThreshold = 100;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Calculate the grayscale intensity (ignoring the alpha channel)
                int intensity = (red + green + blue) / 3;

                if (intensity < darkPixelThreshold) {
                    darkPixelCount++;
                }
            }
        }

        return (double) darkPixelCount / totalPixels * 100;  // Return percentage of dark pixels
    }

    // Calculate the contrast of an ARGB_8888 image
    public static double calculateContrast(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int totalPixels = width * height;

        int[] pixelIntensity = new int[256];  // Array to store pixel intensities from 0-255
        long totalBrightness = 0;

        // Analyze pixel intensities
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = bitmap.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Calculate the grayscale intensity (ignoring alpha channel)
                int intensity = (red + green + blue) / 3;

                pixelIntensity[intensity]++;
                totalBrightness += intensity;
            }
        }

        double avgBrightness = (double) totalBrightness / totalPixels;
        double contrast = 0;

        // Compute contrast using the variance method
        for (int i = 0; i < 256; i++) {
            contrast += Math.pow(i - avgBrightness, 2) * pixelIntensity[i];
        }

        contrast /= totalPixels;
        return Math.sqrt(contrast);  // Higher value indicates higher contrast
    }
}
