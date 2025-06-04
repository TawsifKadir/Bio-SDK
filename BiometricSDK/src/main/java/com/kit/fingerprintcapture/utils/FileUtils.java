package com.kit.fingerprintcapture.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String BASE_DIR = "bio_reg";

    /**
     * Save a byte array to file.
     * @param byteArray Byte array to save.
     * @param fileName  Name of the file.
     * @param context   Application context.
     */
    public static void saveByteArrayToFile(byte[] byteArray, String fileName, Context context) {
        try {
            // Get the app-specific external storage directory (Scoped Storage)
            File storageDir = new File(context.getExternalFilesDir(null), BASE_DIR);

            // Ensure the directory exists
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // Create the file
            File file = new File(storageDir, fileName + ".dat");

            // Write byte array to the file
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(byteArray);
            fos.close();

            Log.d(TAG, "Byte array saved successfully at: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving byte array to file", e);
        }
    }

    /**
     * Save a Bitmap to file.
     * @param bitmap   Bitmap to save.
     * @param fileName Name of the file.
     * @param context  Application context.
     */
    public static void saveBitmapToFile(Bitmap bitmap, String fileName, Context context) {
        try {
            // Get the app-specific external storage directory (Scoped Storage)
            File storageDir = new File(context.getExternalFilesDir(null), BASE_DIR);

            // Ensure the directory exists
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // Create the file
            File file = new File(storageDir, fileName + ".png");

            // Write the bitmap to the file
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);  // PNG format with 100% quality
            fos.close();

            Log.d(TAG, "Bitmap saved successfully at: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving bitmap to file", e);
        }
    }

    /**
     * Convert Bitmap to byte array.
     * @param bitmap Bitmap to convert.
     * @return Byte array of the Bitmap.
     */
    public static byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);  // PNG format with 100% quality
        return byteArrayOutputStream.toByteArray();
    }
}

