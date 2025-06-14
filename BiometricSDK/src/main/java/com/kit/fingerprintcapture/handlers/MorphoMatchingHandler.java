package com.kit.fingerprintcapture.handlers;

import static androidx.core.content.ContextCompat.getSystemService;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.dermalog.afis.fingercode3.TemplateFormat;
import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.template.TemplateExtractor;
import com.machinezoo.sourceafis.FingerprintImage;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import SecuGen.FDxSDKPro.JSGFPLib;
import SecuGen.FDxSDKPro.SGFDxErrorCode;
import SecuGen.FDxSDKPro.SGFDxSecurityLevel;
import SecuGen.FDxSDKPro.SGFDxTemplateFormat;
import SecuGen.FDxSDKPro.SGFingerInfo;
import SecuGen.FDxSDKPro.SGImpressionType;
public class MorphoMatchingHandler implements IFingerMatcher {
    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;
    private HashMap<FingerprintID,FingerprintTemplate> templateList;
    private FingerprintMatcher mFPMatcher = null;

    public MorphoMatchingHandler(Activity mActivity) {
        this.mActivity = mActivity;
        this.isInitialized = false;
        this.templateList = new HashMap<>();
        this.mFPMatcher = new FingerprintMatcher();

    }

    public long verifyFingerPrint(FingerprintID nowID,byte[] nowImage, int nowWidth, int nowHeight,boolean[] matched){

        long result = -1;
        boolean isError = false;
        Throwable errorObject = null;
        FingerprintTemplate nowFPTemplate = null;
        FingerprintTemplate toMatch = null;
        if(BuildConfig.isDebug) {
            Log.d(TAG, "Entered verifiyFingerprint");
        }
        if(matched==null) return result;

        matched[0] = false;


        try {
            if(BuildConfig.isDebug) {
                Log.d(TAG, "Preparing template");
            }
            nowFPTemplate = new FingerprintTemplate();
            toMatch = nowFPTemplate.dpi(500).create(nowImage);

            if(BuildConfig.isDebug) {
                Log.d(TAG, "Template prepared");
            }

            if(mFPMatcher!=null){
                mFPMatcher = new FingerprintMatcher();
            }

            if(!templateList.isEmpty()){

                Iterator nowIterator = templateList.entrySet().iterator();
                while (nowIterator.hasNext()) {
                    Map.Entry mapElement = (Map.Entry) nowIterator.next();
                    if ((mapElement.getKey()) == nowID) continue;
                    FingerprintTemplate existingTemplate = (FingerprintTemplate) mapElement.getValue();
                    mFPMatcher.index(existingTemplate);
                }

            }
            if(BuildConfig.isDebug) {
                Log.d(TAG, "Performing match");
            }
            double nowMatch = mFPMatcher.match(nowFPTemplate);
            if(BuildConfig.isDebug) {
                Log.d(TAG, "Match done. Result : " + nowMatch);
            }
            matched[0] = (nowMatch>40)?true:false;

            if(!matched[0]){
                templateList.put(nowID,nowFPTemplate);
            }else{
                templateList.put(nowID,nowFPTemplate);
            }

            result = 0;

        }catch(Throwable t){
            isError=true;
            errorObject=t;

        }finally {
            if(isError){
                Log.e(TAG, "Verify Fingerprint Error : "+errorObject.getMessage());
                errorObject.printStackTrace();
                result = -1;
                errorObject = null;
            }
            nowFPTemplate = null;
            toMatch = null;
        }

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Leaving verifiyFingerprint");
        }
        return result;

    }


    @Override
    public void verifyFingerPrint(Integer fingerprintId, ISOTemplate searchTemplate,
                                  List<ISOTemplate> referenceTemplateList,
                                  List<MatchResult> result,
                                  TemplateFormat subjectTmplType,
                                  TemplateFormat candidateTmplType) {
        throw new UnsupportedOperationException("Image matching not supported in Morpho matcher.");
    }


    public void compareWithAllStoredTemplates(byte[] nowImage, int nowWidth, int nowHeight) {
        try {
            if (nowImage == null || nowImage.length == 0) {
                Log.e(TAG, "Empty fingerprint image. Cannot compare.");
                return;
            }

            // Create template from image
            FingerprintTemplate currentTemplate = new FingerprintTemplate()
                    .dpi(500)
                    .create(nowImage);

            // Iterate and compare with each stored template
            for (Map.Entry<FingerprintID, FingerprintTemplate> entry : templateList.entrySet()) {
                FingerprintID id = entry.getKey();
                FingerprintTemplate storedTemplate = entry.getValue();

                // Compare using matcher
                FingerprintMatcher matcher = new FingerprintMatcher()
                        .index(storedTemplate);

                double score = matcher.match(currentTemplate);

                // Log result
                Log.d(TAG, "Match Score with " + id.getName() + ": " + score);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error during comparison: " + e.getMessage(), e);
        }
    }



}