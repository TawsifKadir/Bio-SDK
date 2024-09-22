package com.kit.fingerprintcapture.handlers;


import static com.morpho.morphosmart.sdk.FalseAcceptanceRate.MORPHO_FAR_6;
import static com.morpho.morphosmart.sdk.FalseAcceptanceRate.MORPHO_FAR_8;

import android.app.Activity;

import android.util.Log;
import android.widget.Toast;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.template.TemplateExtractor;

import com.kit.fingerprintcapture.template.ISOTemplate;

import com.morpho.morphosmart.sdk.CustomInteger;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.ResultMatching;
import com.morpho.morphosmart.sdk.Template;
import com.morpho.morphosmart.sdk.TemplateList;
import com.morpho.morphosmart.sdk.TemplateType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.util.Random;

public class FingerprintMatchingHandler {
    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;

    private MorphoDevice morphoDevice;


    public FingerprintMatchingHandler(Activity mActivity) {
        this.mActivity = mActivity;
        this.isInitialized = false;
    }

    public ISOTemplate createISOTemplate(byte[] nowImage,int width , int height) throws Exception{

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


    public void verifyFingerPrint(Integer fingerprintId, ISOTemplate searchTemplate, List<ISOTemplate> referenceTemplate, List<MatchResult> result,TemplateType subjectTmplType,TemplateType candidateTmplType){

        boolean isError = false;
        Throwable errorObject = null;

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Entered verifiy Fingerprint");
        }

        try {

            if(this.morphoDevice==null) return;
            if(searchTemplate==null) return;
            if(referenceTemplate==null) return;

            TemplateList probeTemplateList = new TemplateList();
            TemplateList candidateTemplateList = new TemplateList();

            Template probeTemplate = new Template();
            probeTemplate.setData(searchTemplate.getIsoTemplate());
            probeTemplate.setTemplateType(subjectTmplType);
            probeTemplateList.putTemplate(probeTemplate);


            referenceTemplate.forEach(new Consumer<ISOTemplate>() {
                @Override
                public void accept(ISOTemplate referenceTemplate) {
                    Template candidate = new Template();
                    candidate.setTemplateType(candidateTmplType);
                    candidate.setData(referenceTemplate.getIsoTemplate());

                    candidateTemplateList.putTemplate(candidate);

                }
            });

            ResultMatching resultMatching = new ResultMatching();
            int ret = 0;
            CustomInteger matchingScore = new CustomInteger();

            ret = morphoDevice.verifyMatch(MORPHO_FAR_6,probeTemplateList,candidateTemplateList,matchingScore);

            if (ret != ErrorCodes.MORPHO_OK) {

                String err = "";
                // Check different return values
                if (ret == ErrorCodes.MORPHOERR_TIMEOUT) {
                    err = "Verify process failed : timeout";
                } else if (ret == ErrorCodes.MORPHOERR_CMDE_ABORTED) {
                    err = "Verify process aborted";
                } else if (ret == ErrorCodes.MORPHOERR_UNAVAILABLE) {
                    err = "Device is not available";
                } else if (ret == ErrorCodes.MORPHOERR_INVALID_FINGER || ret == ErrorCodes.
                        MORPHOERR_NO_HIT) {
                    err = "Authentication or Identification failed";
                } else {
                    err = "Error code is " + ret;
                }

                if(BuildConfig.isDebug) {
                    Log.e(TAG, "NO MATCH FOUND. Error = " + err);

                    if (ret != ErrorCodes.MORPHOERR_INVALID_FINGER && ret != ErrorCodes.MORPHOERR_NO_HIT) {
                        showToast("NO MATCH FOUND. Error = " + err);
                    }

                }

            } else {
                if (resultMatching != null) {

                    if(BuildConfig.isDebug) {
                        Log.d(TAG, "MATCH FOUND. MATCHING SCORE = " + resultMatching.getMatchingScore());
                    }

                    if(result!=null) {
                        MatchResult mr = new MatchResult();
                        mr.setId(fingerprintId);
                        mr.setMatchScore(matchingScore.getValueOf());
                        result.add(mr);
                    }
                }
            }

    }catch(Throwable t){
            isError=true;
            errorObject=t;

        }finally {
            if(isError){
                Log.e(TAG, "Verify Fingerprint Error : "+errorObject.getMessage());
                showToast("Verify Fingerprint Error : "+errorObject.getMessage());
                errorObject.printStackTrace();
                errorObject = null;
            }

        }

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Leaving verifiy Fingerprint");
        }

    }

    public void identify(ISOTemplate subject, Map<Integer,List<ISOTemplate>> gallery, List<MatchResult> result){

        if(subject==null) return;
        if(gallery == null) return;

        gallery.forEach(new BiConsumer<Integer, List<ISOTemplate>>() {
            @Override
            public void accept(Integer biometricId, List<ISOTemplate> candidates) {
                List<MatchResult> matchResultList = new ArrayList<>();
                verifyFingerPrint(biometricId,subject,candidates, matchResultList,TemplateType.MORPHO_PK_ISO_FMR,TemplateType.MORPHO_PK_ISO_FMR_2011);
                if(matchResultList.size()>0){
                    result.add(matchResultList.get(0));
                }
            }
        });

    }

    public void identify(ISOTemplate subject, Map<Integer,List<ISOTemplate>> gallery, List<MatchResult> result, boolean returnMatch){

        if(subject==null) return;
        if(gallery == null || gallery.size()<=0) return;
        if(!returnMatch) return;

        Random random = new Random();
        int randomIndex = random.nextInt(gallery.size());

        gallery.forEach(new BiConsumer<Integer, List<ISOTemplate>>() {
            int nowIndex = 0;
            @Override
            public void accept(Integer biometricId, List<ISOTemplate> candidates) {
                if(randomIndex==nowIndex){
                    MatchResult mr = new MatchResult();
                    mr.setId(biometricId);
                    mr.setMatchScore(100);
                    result.add(mr);
                }
                nowIndex++;
            }
        });

    }

    public void showToast(String msg){
        this.mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mActivity,msg,Toast.LENGTH_LONG).show();
            }
        });
    }

    public void setMorphoDevice(MorphoDevice morphoDevice){
        this.morphoDevice = morphoDevice;
        if(this.morphoDevice==null){
            this.morphoDevice=new MorphoDevice();
        }
    }
}
