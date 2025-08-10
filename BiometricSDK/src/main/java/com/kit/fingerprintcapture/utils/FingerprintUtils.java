package com.kit.fingerprintcapture.utils;

import android.util.Log;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.TemplateExtractor;

public class FingerprintUtils {

    public static int Height = 448;
    public static int Width = 248;

    static String TAG = "FingerPrintUtils";


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
