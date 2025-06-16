package com.kit.fingerprintcapture.utils;

import android.util.Log;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.TemplateExtractor;

public class TemplateUtils {
    public static final String TAG = "TemplateUtils";


    public static ISOTemplate createISOTemplate(byte[] nowImage, int width , int height) throws Exception{

        byte[][] fmd = new byte[1][1000 + 256 * 6];

        int[] fmdSize = new int[1];
        fmdSize[0] = 1000 + 256 * 6;

        ISOTemplate fmdTmpl = new ISOTemplate(null,0);

        int ret = TemplateExtractor.getMyInstance().createFmdFromRaw(nowImage,500,height,width,TemplateExtractor.FJFX_FMD_ISO_19794_2_2005,fmd[0],fmdSize);

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
}
