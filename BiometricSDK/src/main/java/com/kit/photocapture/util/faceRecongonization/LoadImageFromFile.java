package com.kit.photocapture.util.faceRecongonization;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;

import com.kit.photocapture.model.detector.FaceDetectionModel;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;

public class LoadImageFromFile {


    public static Bitmap loadBitmapFromFile(Context context, String filename) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) + "/SavedPhotos", filename);
        return file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
    }


}
