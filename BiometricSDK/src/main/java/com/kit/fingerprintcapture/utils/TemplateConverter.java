package com.kit.fingerprintcapture.utils;

import android.util.Log;

import com.kit.fingerprintcapture.template.ISOTemplate;
import com.machinezoo.fingerprintio.TemplateFormat;
import com.machinezoo.sourceafis.FingerprintCompatibility;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.io.IOException;

public class TemplateConverter {
    private static String TAG = "TemplateConverter";
    /**
     * Converts fingerprint template data from ISO 19794-2:2011 to ISO 19794-2:2005.
     *
     * @param iso2011Data Byte array containing the fingerprint template in ISO 19794-2:2011 format.
     * @return Byte array containing the converted fingerprint template in ISO 19794-2:2005 format.
     * @throws IOException If an I/O error occurs during conversion.
     */
    public static byte[] convertIso2011toIso2005(byte[] iso2011Data) throws IOException {
        FingerprintTemplate iso2011Template = FingerprintCompatibility.importTemplate(iso2011Data);
        byte[] iso2005Tpl = FingerprintCompatibility.exportTemplates(TemplateFormat.ISO_19794_2_2005,iso2011Template);
        return iso2005Tpl;
    }

    public static ISOTemplate checkAndGetISO2005Version(ISOTemplate iso2011Data) throws IOException {


        if(iso2011Data.getIsoTemplate().length <= 0)
        {
            return null;
        }
        if(TemplateConverter.isISO2005(iso2011Data.getIsoTemplate())){
            return iso2011Data;
        }
        else if(TemplateConverter.isISO2011(iso2011Data.getIsoTemplate())){

            byte[] temp = TemplateConverter.convertIso2011toIso2005(iso2011Data.getIsoTemplate());
            return new ISOTemplate(temp, iso2011Data.getIsoTemplate().length);

        }

        return iso2011Data; //if not any case
    }

    /**
     * Converts fingerprint template data from ISO 19794-2:2005 to ISO 19794-2:2011.
     *
     * @param iso2005Data Byte array containing the fingerprint template in ISO 19794-2:2005 format.
     * @return Byte array containing the converted fingerprint template in ISO 19794-2:2011 format.
     * @throws IOException If an I/O error occurs during conversion.
     */
    public static byte[] convertIso2005toIso2011(byte[] iso2005Data) throws IOException {
        FingerprintTemplate iso2005Template = FingerprintCompatibility.importTemplate(iso2005Data);
        byte[] iso2011Tpl = FingerprintCompatibility.exportTemplates(TemplateFormat.ISO_19794_2_2011,iso2005Template);
        return iso2011Tpl;
    }


    public static ISOTemplate checkAndGetISO2011Version(ISOTemplate iso2005Data) throws IOException {


        if(iso2005Data.getIsoTemplate().length <= 0)
        {
            return null;
        }
        if(TemplateConverter.isISO2005(iso2005Data.getIsoTemplate())){
            byte[] temp = TemplateConverter.convertIso2005toIso2011(iso2005Data.getIsoTemplate());
            return new ISOTemplate(temp, iso2005Data.getIsoTemplate().length);
        }
        else if(TemplateConverter.isISO2011(iso2005Data.getIsoTemplate())){
            return iso2005Data;
        }

        return iso2005Data;
    }
    public static boolean isISO2011(byte[] tpl){
        if(tpl==null) return false;
        try{
            if(TemplateFormat.identify(tpl) == TemplateFormat.ISO_19794_2_2011) return true;
        }catch(Throwable t){
            Log.e(TAG,"Error in template checking : "+t.getMessage());
        }
        return false;
    }
    public static boolean isISO2005(byte[] tpl){
        if(tpl==null) return false;
        try{
            if(TemplateFormat.identify(tpl) == TemplateFormat.ISO_19794_2_2005) return true;
        }catch(Throwable t){
            Log.e(TAG,"Error in template checking : "+t.getMessage());
        }
        return false;
    }
    public static boolean isANSI2004(byte[] tpl){
        if(tpl==null) return false;
        try{
            if(TemplateFormat.identify(tpl) == TemplateFormat.ANSI_378_2004) return true;
        }catch(Throwable t){
            Log.e(TAG,"Error in template checking : "+t.getMessage());
        }
        return false;
    }

    public static boolean isANSI2009(byte[] tpl){
        if(tpl==null) return false;
        try{
            if(TemplateFormat.identify(tpl) == TemplateFormat.ANSI_378_2009) return true;
        }catch(Throwable t){
            Log.e(TAG,"Error in template checking : "+t.getMessage());
        }
        return false;
    }
    public static boolean isANSI2009AM1(byte[] tpl){
        if(tpl==null) return false;
        try{
            if(TemplateFormat.identify(tpl) == TemplateFormat.ANSI_378_2009_AM1) return true;
        }catch(Throwable t){
            Log.e(TAG,"Error in template checking : "+t.getMessage());
        }
        return false;
    }
}


