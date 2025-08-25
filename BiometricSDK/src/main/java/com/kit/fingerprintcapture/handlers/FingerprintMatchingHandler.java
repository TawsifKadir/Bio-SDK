package com.kit.fingerprintcapture.handlers;

import static com.morpho.morphosmart.sdk.FalseAcceptanceRate.MORPHO_FAR_6;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;
import com.kit.fingerprintcapture.template.MatchResult;
import com.kit.fingerprintcapture.utils.TemplateConverter;
import com.kit.fingerprintcapture.utils.TemplateUtils;
import com.morpho.morphosmart.sdk.CustomInteger;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.Template;
import com.morpho.morphosmart.sdk.TemplateList;
import com.morpho.morphosmart.sdk.TemplateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FingerprintMatchingHandler {

    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;
    private HashMap<FingerprintID,ISOTemplate> templateList;

    private MorphoDevice morphoDevice;

    static public double MATCH_THRESHOULD = 20.0;

    public HashMap<FingerprintID, ISOTemplate> getTemplateList() {
        return templateList;
    }

    public void setTemplateList(HashMap<FingerprintID, ISOTemplate> templateList) {
        this.templateList = templateList;
    }

    public FingerprintMatchingHandler(Activity mActivity) {
        this.mActivity = mActivity;
        this.isInitialized = false;
        this.templateList = new HashMap<>();
    }

    public long verifyFingerPrint(FingerprintID nowID, byte[] nowImage, int nowWidth, int nowHeight, boolean[] matched) {
        Log.d(TAG, "Entered verifyFingerPrint (Morpho-based)");

        long result = -1;

        // Validate inputs
        if (matched == null || nowImage == null || nowWidth <= 0 || nowHeight <= 0) {
            Log.d(TAG, "⚠️ Invalid input: matched=null or invalid image data");
            return result;
        }

        matched[0] = false;

        if (morphoDevice == null) {
            Log.e(TAG, "⚠️ Morpho device not initialized");
            return result;
        }

        ISOTemplate nowFPTemplate = null;

        try {
            // Build probe template (from the new fingerprint image)
            nowFPTemplate = TemplateUtils.createISOTemplate(nowImage, nowWidth, nowHeight);

            Template probeTemplate = new Template();
            probeTemplate.setTemplateType(TemplateType.MORPHO_PK_ISO_FMR);
            probeTemplate.setData(nowFPTemplate.getIsoTemplate());

            TemplateList probeTemplateList = new TemplateList();
            probeTemplateList.putTemplate(probeTemplate);

            // Build candidate templates from our reference list
            TemplateList candidateTemplateList = new TemplateList();
            for (Map.Entry<FingerprintID, ISOTemplate> entry : templateList.entrySet()) {
                if (entry.getKey().equals(nowID)) continue; // skip same finger

                ISOTemplate refTemplate = entry.getValue();

                Template candidate = new Template();
                candidate.setTemplateType(TemplateType.MORPHO_PK_ISO_FMR);
                candidate.setData(refTemplate.getIsoTemplate());

                Log.d(TAG, "Template type is 2005?: " + TemplateConverter.isISO2005(refTemplate.getIsoTemplate()));
                Log.d(TAG, "Template type is 2011?: " + TemplateConverter.isISO2011(refTemplate.getIsoTemplate()));

                candidateTemplateList.putTemplate(candidate);
            }

            // Perform verification
            CustomInteger matchingScore = new CustomInteger();
            int ret = morphoDevice.verifyMatch(
                    MORPHO_FAR_6,
                    probeTemplateList,
                    candidateTemplateList,
                    matchingScore
            );

            if (ret == ErrorCodes.MORPHO_OK) {
                int score = matchingScore.getValueOf();
                Log.d(TAG, "✅ MATCH FOUND. Score = " + score);
                matched[0] = true;
                result = 0;
            } else {
                String err;
                switch (ret) {
                    case ErrorCodes.MORPHOERR_TIMEOUT:
                        err = "Verify failed: timeout"; break;
                    case ErrorCodes.MORPHOERR_CMDE_ABORTED:
                        err = "Verify aborted"; break;
                    case ErrorCodes.MORPHOERR_UNAVAILABLE:
                        err = "Device unavailable"; break;
                    case ErrorCodes.MORPHOERR_INVALID_FINGER:
                    case ErrorCodes.MORPHOERR_NO_HIT:
                        err = "Authentication failed (no match)"; break;
                    default:
                        err = "Error code: " + ret;
                }
                Log.e(TAG, "❌ NO MATCH FOUND. " + err);

                // 👉 Add the probe template to the list if not matched
                templateList.put(nowID, nowFPTemplate);
                Log.d(TAG, "New template added to templateList. Current size=" + templateList.size());

                result = -1;
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in verifyFingerPrint: " + e.getMessage(), e);
            result = -1;
        } finally {
            Log.d(TAG, "Leaving verifyFingerPrint");
        }

        return result;
    }




//    public void verifyFingerPrint2(Integer fingerprintId, FingerprintTemplate searchTemplate,
//                                   List<FingerprintTemplate> referenceFingerTemplateList,
//                                   List<MatchResult> results) {
//        Log.d(TAG, "Entered verifyFingerPrint2");
//
//        // Clear and initialize results
//        if (results == null) {
//            results = new ArrayList<>();
//        } else {
//            results.clear();
//        }
//
//        // Check for null or empty inputs
//        if (searchTemplate == null || referenceFingerTemplateList == null || referenceFingerTemplateList.isEmpty()) {
//            Log.d(TAG, "Null or empty inputs detected");
//            if (referenceFingerTemplateList == null || referenceFingerTemplateList.isEmpty())
//            {
//                Log.d(TAG, "Null or empty inputs detected referenceFingerTemplateList");
//            }
//            return;
//        }
//
//        try {
//            Log.d(TAG, "Starting matching process");
//            FingerprintMatcher matcher = new FingerprintMatcher();
//            matcher.index(searchTemplate);  // Index the search template
//
//            Log.d(TAG, "Fetching reference templates");
//            for (FingerprintTemplate currentTemplate : referenceFingerTemplateList) {
//                if (currentTemplate != null) {
//                    // Perform matching and set the result
//                    double matchScore = matcher.match(currentTemplate);
//                    int intScore = (int) Math.round(matchScore);
//                    Log.d(TAG, "match scorre: "+ intScore);
//
//                    // Only add result if match score exceeds the threshold
//                    if (matchScore >= MATCH_THRESHOULD) {
//                        MatchResult result = new MatchResult();
//                        result.setId(fingerprintId); // Set the provided fingerprint ID
//                        result.setMatchScore(intScore); // Set the calculated match score
//                        results.add(result);
//                        Log.d(TAG, "Match score for template with ID " + FingerprintID.getFingerprintID(fingerprintId) + " is high threshold: " + matchScore);
//                    } else {
//                        Log.d(TAG, "Match score for template with ID " + FingerprintID.getFingerprintID(fingerprintId) + " is below threshold: " + matchScore);
//                    }
//                } else {
//                    Log.d(TAG, "Skipping null reference template");
//                }
//            }
//            Log.d(TAG, "Matching completed");
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error in verifyFingerPrint2: " + e.getMessage(), e);
//            results.clear(); // Clear results on error
//        } finally {
//            if (BuildConfig.isDebug) {
//                Log.d(TAG, "Leaving verifyFingerPrint2");
//            }
//        }
//    }

    public void verifyFingerPrint2(
            Integer candidateFingerId,
            ISOTemplate searchTemplate,
            Map<Integer, ISOTemplate> referenceTemplates,
            List<MatchResult> results) {

        Log.d(TAG, "Entered verifyFingerPrint2");

        // Initialize results list
        if (results == null) {
            results = new ArrayList<>();
        } else {
            results.clear();
        }

        // Validate inputs
        if (searchTemplate == null || referenceTemplates == null || referenceTemplates.isEmpty()) {
            Log.d(TAG, "⚠️ Invalid input: searchTemplate or referenceTemplates is null/empty");
            return;
        }

        if (morphoDevice == null) {
            Log.d(TAG, "⚠️ Morpho device is not initialized");
            return;
        }

        try {
            // Build probe (search) template
            Template probeTemplate = new Template();
            probeTemplate.setTemplateType(TemplateType.MORPHO_PK_ISO_FMR);
            probeTemplate.setData(searchTemplate.getIsoTemplate());

            TemplateList probeTemplateList = new TemplateList();
            probeTemplateList.putTemplate(probeTemplate);

            // Build candidate templates
            TemplateList candidateTemplateList = new TemplateList();
            for (ISOTemplate referenceTemplate : referenceTemplates.values()) {
                Template candidate = new Template();
                candidate.setTemplateType(TemplateType.MORPHO_PK_ISO_FMR);
                candidate.setData(referenceTemplate.getIsoTemplate());
                candidateTemplateList.putTemplate(candidate);
            }

            // Perform verification
            CustomInteger matchingScore = new CustomInteger();
            int ret = morphoDevice.verifyMatch(
                    MORPHO_FAR_6,
                    probeTemplateList,
                    candidateTemplateList,
                    matchingScore
            );

            if (ret == ErrorCodes.MORPHO_OK) {
                int score = matchingScore.getValueOf();
                Log.d(TAG, "✅ MATCH FOUND. Score = " + score);

                MatchResult mr = new MatchResult();
                mr.setId(candidateFingerId);
                mr.setMatchScore(score);
                results.add(mr);

            } else {
                String err;
                switch (ret) {
                    case ErrorCodes.MORPHOERR_TIMEOUT:
                        err = "Verify failed: timeout";
                        break;
                    case ErrorCodes.MORPHOERR_CMDE_ABORTED:
                        err = "Verify aborted";
                        break;
                    case ErrorCodes.MORPHOERR_UNAVAILABLE:
                        err = "Device unavailable";
                        break;
                    case ErrorCodes.MORPHOERR_INVALID_FINGER:
                    case ErrorCodes.MORPHOERR_NO_HIT:
                        err = "Authentication failed (no match)";
                        break;
                    default:
                        err = "Error code: " + ret;
                }

                Log.e(TAG, "❌ NO MATCH FOUND. " + err);
                if (ret != ErrorCodes.MORPHOERR_INVALID_FINGER &&
                        ret != ErrorCodes.MORPHOERR_NO_HIT) {
                    showToast("NO MATCH FOUND. " + err);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Exception in verifyFingerPrint2: " + e.getMessage(), e);
            results.clear();
        } finally {
            Log.d(TAG, "Leaving verifyFingerPrint2");
        }
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
