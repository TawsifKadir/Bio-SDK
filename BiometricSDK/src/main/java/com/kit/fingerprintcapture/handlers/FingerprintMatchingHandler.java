package com.kit.fingerprintcapture.handlers;

import android.app.Activity;

import android.util.Log;
import android.widget.Toast;

import com.dermalog.afis.fingercode3.Matcher;
import com.dermalog.afis.fingercode3.Template;

import com.dermalog.afis.fingercode3.TemplateFormat;
import com.kit.BuildConfig;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.template.TemplateExtractor;

import com.kit.fingerprintcapture.template.ISOTemplate;

import java.util.ArrayList;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import java.util.Random;

public class FingerprintMatchingHandler {
    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;


    private Matcher matcher;

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


    public void verifyFingerPrint(Integer fingerprintId, ISOTemplate searchTemplate, List<ISOTemplate> referenceTemplateList, List<MatchResult> result,TemplateFormat subjectTmplType,TemplateFormat candidateTmplType){

        boolean isError = false;
        Throwable errorObject = null;
        List<Double> matchScoreList = new ArrayList<>();

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Entered verifiy Fingerprint");
        }

        try {

            if(this.matcher==null) return;
            if(searchTemplate==null) return;
            if(referenceTemplateList==null) return;


            Template probeTemplate = new Template();
            TemplateFormat testFormat = TemplateFormat.ISO19794_2_2005;
            probeTemplate.SetData(searchTemplate.getIsoTemplate(),testFormat);

            referenceTemplateList.forEach(new Consumer<ISOTemplate>() {
                @Override
                public void accept(ISOTemplate referenceTemplate) {
                    try {
                        Template candidate = new Template();
//                        candidate.setFormat(candidateTmplType);
//                        candidate.setData(referenceTemplate.getIsoTemplate());
                        candidate.SetData(referenceTemplate.getIsoTemplate(),testFormat);
                        double nowScore = matcher.Match(probeTemplate,candidate);
                        if(nowScore>=30) {
                            matchScoreList.add(nowScore);
                        }
                        Log.d(TAG, "accept() called with score: " + nowScore);
                    }catch(Throwable t){
                        Log.e(TAG, "Verify Fingerprint Error while matching : "+t.getMessage());
                        showToast("Verify Fingerprint Error while matching : "+t.getMessage());
                    }
                }
            });

            if(!matchScoreList.isEmpty()){
                MatchResult mr = new MatchResult();
                mr.setId(fingerprintId);

                Optional score = matchScoreList.stream().max(new Comparator<Double>() {
                    @Override
                    public int compare(Double o1, Double o2) {
                        if(o1>o2) return o1.intValue();
                        return o2.intValue();
                    }
                });
                mr.setMatchScore((int)score.get());
                result.add(mr);

            }


    }catch(Throwable t){
            isError=true;
            errorObject=t;

        }finally {
            if(isError){
                Log.e(TAG, "Verify Fingerprint Error after matching: "+errorObject.getMessage());
                showToast("Verify Fingerprint Error after matching: "+errorObject.getMessage());
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
                verifyFingerPrint(biometricId,subject,candidates, matchResultList,TemplateFormat.ISO19794_2_2005, TemplateFormat.ISO19794_2_2005);
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

    public void setMatcher(Matcher matcher){
        this.matcher = matcher;
    }
}
