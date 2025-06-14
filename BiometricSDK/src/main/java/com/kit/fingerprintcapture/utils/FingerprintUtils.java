package com.kit.fingerprintcapture.utils;

import android.util.Log;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.TemplateExtractor;

public class FingerprintUtils {



    String TAG = "FingerUtils";
    public ISOTemplate createISOTemplate(byte[] nowImage, int width , int height) throws Exception{

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

                if (BuildConfig.isDebug) {
                    Log.d(TAG, "ISO Template created successfully. Size: " + fmdSize[0]);
                    Log.d(TAG, "ISO Template first 20 bytes: " + bytesToHex(retFmd, 20));
                }
            }
        }else{
            throw new Exception("Error in creating ISO template. Error code = "+ret);
        }

        return fmdTmpl;
    }

    public ISOTemplate createANSITemplate(byte[] nowImage,int width , int height) throws Exception{
        byte[][] fmd = new byte[1][1000 + 256 * 6];

        int[] fmdSize = new int[1];
        fmdSize[0] = 1000 + 256 * 6;

        ISOTemplate fmdTmpl = new ISOTemplate(null,0);


        int ret = TemplateExtractor.getMyInstance().createFmdFromRaw(nowImage,500,height,width,TemplateExtractor.FJFX_FMD_ANSI_378_2004,fmd[0],fmdSize);

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

    private String bytesToHex(byte[] bytes, int limit) {
        StringBuilder sb = new StringBuilder();
        int len = Math.min(limit, bytes.length);
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString();
    }


}
