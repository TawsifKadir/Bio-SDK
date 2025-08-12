package com.kit.fingerprintcapture.utils;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import com.MyApplication;
import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.TemplateExtractor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FingerprintUtils {

    public static int Height = 448;
    public static int Width = 248;

    static String TAG = "FingerPrintUtils";

    // Utils — put in a helper class (e.g., ImageUtils)
    public static Bitmap grayscaleToBitmap(byte[] data, int width, int height) {
        int[] colors = new int[width * height];
        for (int i = 0; i < data.length; i++) {
            int g = data[i] & 0xFF;
            colors[i] = 0xFF000000 | (g << 16) | (g << 8) | g; // ARGB from grayscale
        }
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(colors, 0, width, 0, 0, width, height);
        return bmp;
    }

    public static Uri saveFingerprintImage(byte[] decoded, int width, int height, String fileName)
            throws IOException {

        Bitmap bmp = grayscaleToBitmap(decoded, width, height);
        Context ctx = MyApplication.getAppContext(); // Your global app context

        // Create internal storage folder
        File dir = new File(ctx.getFilesDir(), "bio_reg");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Failed to create bio_reg directory");
        }

        // Save the file inside bio_reg folder
        File outFile = new File(dir, fileName);
        try (FileOutputStream os = new FileOutputStream(outFile)) {
            if (!bmp.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                throw new IOException("Bitmap compress failed");
            }
        }

        Log.d("FingerprintSave", "Saved to: " + outFile.getAbsolutePath());
        return Uri.fromFile(outFile); // Internal file URI
    }

    public static FingerprintData imageToFingerprintDataModel(byte[] imgData,int score, int width , int height, FingerprintID fingerprintID) {
        if (imgData == null || fingerprintID == null) {
            return null;
        }

        try {
            // Compute quality score
//            long imScore = ImageProc.computeScore(imgData, width, height);
//            long mappedScore = ImageProc.mapNFIQScore((int) imScore);

            // Create WSQ (optional - for storage/display)
            byte[] wsqData = ImageProc.toWSQ(imgData, width, height);

            // Create ISO Template
            ISOTemplate isoTemplateObj = TemplateUtils.createISOTemplate(imgData, width, height);
            byte[] isoTemplateBytes = isoTemplateObj != null ? isoTemplateObj.getIsoTemplate() : new byte[0];

            // Assemble FingerprintData
            FingerprintData fingerprintData = new FingerprintData();
            fingerprintData.setFingerprintId(fingerprintID);
            fingerprintData.setFingerprintData(wsqData);
            fingerprintData.setQualityScore(score);
            fingerprintData.setIsoTemplate(isoTemplateBytes);

            return fingerprintData;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }




/*
    public static ISOTemplate rawImageToISO(byte[] nowImage,int width,int height) throws Exception{

        byte[][] fmd = new byte[1][1000 + 256 * 6];

        int[] fmdSize = new int[1];
        fmdSize[0] = 1000 + 256 * 6;

        ISOTemplate fmdTmpl = new ISOTemplate(null,0);

        int ret = TemplateExtractor.getMyInstance().createFmdFromRaw(nowImage,500,height, width,TemplateExtractor.FJFX_FMD_ISO_19794_2_2005,fmd[0],fmdSize);

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Template Extractor returned : " + ret);
        }

        if(ret==TemplateExtractor.FJFX_SUCCESS) {
            if (fmdSize[0] > 0) {
                byte[] retFmd = new byte[fmdSize[0]];
                System.arraycopy(fmd[0], 0, retFmd, 0, fmdSize[0]);
                fmdTmpl.setIsoTemplate(retFmd);
                fmdTmpl.setIsoTemplateSize(fmdSize[0]);
            }
        }else{
            throw new Exception("Error in creating ISO template. Error code = "+ret);
        }

        return fmdTmpl;
    }
*/

}
