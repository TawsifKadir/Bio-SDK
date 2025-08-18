package com.kit.fingerprintcapture.utils;

import static com.kit.fingerprintcapture.utils.FingerprintsManager.nowHeight;
import static com.kit.fingerprintcapture.utils.FingerprintsManager.nowWidth;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

import SecuGen.FDxSDKPro.SGWSQLib;

public class ImageProc {
    private static SGWSQLib wsqLib;
    static{
        wsqLib = new SGWSQLib();
    }

    public static class DecodedImage {
        public final byte[] pixels;
        public final int width;
        public final int height;
        public final Bitmap bitmap; // optional

        public DecodedImage(byte[] pixels, int width, int height, Bitmap bitmap) {
            this.pixels = pixels;
            this.width = width;
            this.height = height;
            this.bitmap = bitmap;
        }
    }

    public static Bitmap toGrayscale(byte[] mImageBuffer, int width, int height)
    {
        byte[] Bits = new byte[mImageBuffer.length * 4];
        for (int i = 0; i < mImageBuffer.length; i++) {
            Bits[i * 4] = Bits[i * 4 + 1] = Bits[i * 4 + 2] = mImageBuffer[i]; // Invert the source bits
            Bits[i * 4 + 3] = -1;// 0xff, that's the alpha.
        }

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmpGrayscale.copyPixelsFromBuffer(ByteBuffer.wrap(Bits));
        return bmpGrayscale;
    }

    public static byte[] toWSQ(byte[] mImageBuffer, int width, int height){

        if(mImageBuffer==null){
            return null;
        }

        if(width<=0||height<=0){
            return null;
        }

        int wsqImageOutSize[] = new int[1] ;
        wsqLib.SGWSQGetEncodedImageSize(wsqImageOutSize,SGWSQLib.BITRATE_15_TO_1, mImageBuffer,width,height,8, 500);
        byte[] wsqData = new byte[wsqImageOutSize[0]];
        wsqLib.SGWSQEncode(wsqData,SGWSQLib.BITRATE_15_TO_1,mImageBuffer,width,height,8,500);
        return wsqData;
    }
    public static byte[] fromWSQ(byte[] wsqBuffer, int width ,int height){

        if(wsqBuffer==null){
            return null;
        }

        if(width<=0||height<=0){
            return null;
        }

        int[] greyImageOutSize = new int[1];

        long error = wsqLib.SGWSQGetDecodedImageSize(greyImageOutSize,wsqBuffer,wsqBuffer.length);

        byte[] greyData = new byte[greyImageOutSize[0]];

        int[] oWidth = new int[1];
        int[] oHeight = new int[1];
        int[] oPixelDepth = new int[1];
        int[] oPpi = new int[1];
        int[] oLossyFlag = new int[1];

        error = wsqLib.SGWSQDecode(greyData,oWidth,oHeight,oPixelDepth,oPpi,oLossyFlag,wsqBuffer,wsqBuffer.length);

        return greyData;
    }


    public static DecodedImage fromWSQ(byte[] wsqBuffer) {
        if (wsqBuffer == null || wsqBuffer.length == 0) {
            return new DecodedImage(new byte[0], 0, 0, null);
        }

        try {
            int[] width = new int[1];
            int[] height = new int[1];
            int[] depth = new int[1];
            int[] ppi = new int[1];
            int[] lossyFlag = new int[1];

            // Step 1: Get decoded size
            int[] outSize = new int[1];
            long err = wsqLib.SGWSQGetDecodedImageSize(outSize, wsqBuffer, wsqBuffer.length);
            if (err != 0) {
                Log.e("ImageProc", "❌ WSQGetDecodedImageSize failed, err=" + err);
                return new DecodedImage(new byte[0], 0, 0, null);
            }

            byte[] pixels = new byte[outSize[0]];

            // Step 2: Decode WSQ into pixels
            err = wsqLib.SGWSQDecode(pixels, width, height, depth, ppi, lossyFlag,
                    wsqBuffer, wsqBuffer.length);
            if (err != 0) {
                Log.e("ImageProc", "❌ WSQDecode failed, err=" + err);
                return new DecodedImage(new byte[0], 0, 0, null);
            }

            Log.d("ImageProc", "✅ Decoded WSQ → width=" + width[0] +
                    ", height=" + height[0] +
                    ", pixels=" + pixels.length);

            // Step 3: Convert raw grayscale pixels into Bitmap
            Bitmap bitmap = null;
            try {
                bitmap = Bitmap.createBitmap(width[0], height[0], Bitmap.Config.ALPHA_8);
                bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));
            } catch (Throwable t) {
                Log.w("ImageProc", "⚠️ Could not create bitmap from WSQ pixels", t);
            }

            return new DecodedImage(pixels, width[0], height[0], bitmap);

        } catch (Throwable t) {
            Log.e("ImageProc", "Exception decoding WSQ", t);
            return new DecodedImage(new byte[0], 0, 0, null);
        }
    }



    public static DecodedImage decodeBase64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            throw new IllegalArgumentException("Base64 string is empty");
        }

        byte[] imageBytes = Base64.decode(base64Data, Base64.DEFAULT);

        // Detect by "magic bytes"
        if (isWSQ(imageBytes)) {
            return decodeWSQ(imageBytes);
        } else if (isJPEG(imageBytes) || isPNG(imageBytes)) {
            return decodeBitmap(imageBytes);
        } else {
            throw new IllegalArgumentException("❌ Unknown image format: " + Arrays.toString(Arrays.copyOf(imageBytes, 10)));
        }
    }

    private static boolean isWSQ(byte[] data) {
        // WSQ usually starts with 0xFF 0xA0
        return data != null && data.length > 2 &&
                (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xA0;
    }

    private static boolean isJPEG(byte[] data) {
        // JPEG starts with FF D8
        return data != null && data.length > 2 &&
                (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
    }

    private static boolean isPNG(byte[] data) {
        // PNG starts with 89 50 4E 47
        return data != null && data.length > 4 &&
                (data[0] & 0xFF) == 0x89 && (data[1] & 0xFF) == 0x50 &&
                (data[2] & 0xFF) == 0x4E && (data[3] & 0xFF) == 0x47;
    }

    private static DecodedImage decodeWSQ(byte[] wsqBytes) {
        try {
            int[] greyImageOutSize = new int[1];
            long err = ImageProc.wsqLib.SGWSQGetDecodedImageSize(greyImageOutSize, wsqBytes, wsqBytes.length);

            byte[] greyData = new byte[greyImageOutSize[0]];
            int[] oWidth = new int[1];
            int[] oHeight = new int[1];
            int[] oPixelDepth = new int[1];
            int[] oPpi = new int[1];
            int[] oLossyFlag = new int[1];

            err = ImageProc.wsqLib.SGWSQDecode(greyData, oWidth, oHeight, oPixelDepth, oPpi, oLossyFlag, wsqBytes, wsqBytes.length);

            return new DecodedImage(greyData, oWidth[0], oHeight[0], null);

        } catch (Exception e) {
            throw new RuntimeException("WSQ decoding failed", e);
        }
    }

    private static DecodedImage decodeBitmap(byte[] imageBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        if (bitmap == null) {
            throw new IllegalArgumentException("❌ BitmapFactory could not decode image, size=" + imageBytes.length);
        }

        // Convert to raw pixel data if you need it
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int[] pixels = new int[w * h];
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h);

        // You may convert ARGB_8888 → grayscale byte[] if needed
        return new DecodedImage(null, w, h, bitmap);
    }
    public static Bitmap toGrayscale(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        for (int y=0; y< height; ++y) {
            for (int x=0; x< width; ++x){
                int color = bmpOriginal.getPixel(x, y);
                int r = (color >> 16) & 0xFF;
                int g = (color >> 8) & 0xFF;
                int b = color & 0xFF;
                int gray = (r+g+b)/3;
                color = Color.rgb(gray, gray, gray);
                //color = Color.rgb(r/3, g/3, b/3);
                bmpGrayscale.setPixel(x, y, color);
            }
        }
        return bmpGrayscale;
    }

    public static Bitmap toBinary(Bitmap bmpOriginal) {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();
        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }
}
