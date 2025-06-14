package com.kit.fingerprintcapture.handlers;

import android.app.Activity;

import android.util.Log;
import android.widget.Toast;

import com.dermalog.afis.fingercode3.Matcher;
import com.dermalog.afis.fingercode3.Template;

import com.dermalog.afis.fingercode3.TemplateFormat;
import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintID;
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

public class DermalogMatchingHandler implements IFingerMatcher{
    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;


    private Matcher matcher;

    public DermalogMatchingHandler(Activity mActivity) {
        this.mActivity = mActivity;
        this.isInitialized = false;
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
            probeTemplate.SetData(searchTemplate.getIsoTemplate(),subjectTmplType);

            referenceTemplateList.forEach(new Consumer<ISOTemplate>() {
                @Override
                public void accept(ISOTemplate referenceTemplate) {
                    try {
                        Template candidate = new Template();
                        candidate.SetData(referenceTemplate.getIsoTemplate(),candidateTmplType);
                        Log.d(TAG, "Candidate template size is : " + referenceTemplate.getIsoTemplate().length);
                        double nowScore = matcher.Match(probeTemplate,candidate);
                        Log.d(TAG, "Fingerprint Match Score is " + nowScore);
                        if(nowScore>=30) {
                            matchScoreList.add(nowScore);
                        }
                        Log.d(TAG, "accept() called with score: " + nowScore);
                    }catch(Throwable t){
                        Log.e(TAG, "Verify Fingerprint Error while matching : "+t.getMessage());

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
                if(score.isPresent()) {
                    Log.d(TAG, "Matching Score: " + score);
                    mr.setMatchScore( ((Double) score.get()).intValue() );
                    result.add(mr);
                }

            }


    }catch(Throwable t){
            isError=true;
            errorObject=t;

        }finally {
            if(isError){
                Log.e(TAG, "Verify Fingerprint Error after matching: "+errorObject.getMessage());

                errorObject.printStackTrace();
                errorObject = null;
            }

        }

        if(BuildConfig.isDebug) {
            Log.d(TAG, "Leaving verifiy Fingerprint");
        }

    }

    @Override
    public long verifyFingerPrint(FingerprintID nowID, byte[] nowImage, int nowWidth, int nowHeight, boolean[] matched) {
        throw new UnsupportedOperationException("Image matching not supported in Dermalog matcher.");
    }




}
