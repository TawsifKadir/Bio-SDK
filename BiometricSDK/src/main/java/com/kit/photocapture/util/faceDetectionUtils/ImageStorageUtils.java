package com.kit.photocapture.util.faceDetectionUtils;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
public class ImageStorageUtils {

    private static final String TAG = "ImageUtils";

    public static void saveFrame(Context context, Mat frame, String fileName) {
        try {
            Bitmap bmp = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(frame, bmp);

            File path = context.getExternalFilesDir(null);
            File file = new File(path, fileName);
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();

            Log.d(TAG, "✅ Frame saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "❌ Error saving frame", e);
        }
    }
}