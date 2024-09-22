package com.kit.fingerprintcapture.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.util.Log;


import com.seamfix.calculatenfiq.NFIQUtil;

import java.nio.ByteBuffer;

import SecuGen.FDxSDKPro.SGWSQLib;
import SecuGen.FDxSDKPro.SGNFIQLib;

public class ImageProc {
    private static SGWSQLib wsqLib;

    static{
        wsqLib = new SGWSQLib();
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
    public static Bitmap toGrayscale(Bitmap bmpOriginal)
    {
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

    public static Bitmap toBinary(Bitmap bmpOriginal)
    {
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

    public static Long computeScore(byte[] mImageBuffer, int width, int height)
    {
        Long score = (long)-1;

        Log.d("UTILS","Entered to compute score >>>>> ");

        if(mImageBuffer==null || width<=0 || height<=0) return score;

        try{

//            score = nfiqLib.SGComputeNFIQ(mImageBuffer,width,height);
            score = (long) NFIQUtil.calculateNFIQUsingRawBytes(mImageBuffer,width, height);
        }catch(Throwable t){
            t.printStackTrace();
        }

        return score;
    }

    public static int mapNFIQScore(int score){
            switch (score){
                case 1:
                    return 100;
                case 2:
                    return 80;
                case 3:
                    return 60;
                case 4:
                    return 40;
                case 5:
                    return 20;
                default:
                    return 0;

            }
    }

}
