package com.kit.fingerprintcapture.handlers;

import android.app.Activity;
import android.util.Log;

import com.kit.BuildConfig;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.MatchResult;
import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FingerprintMatchingHandler {

    String TAG = "FingerprintMatchingHandler";
    private Activity mActivity;
    private boolean isInitialized;
    private HashMap<FingerprintID,FingerprintTemplate> templateList;
    private FingerprintMatcher mFPMatcher;

    static public double MATCH_THRESHOULD = 20.0;

    public HashMap<FingerprintID, FingerprintTemplate> getTemplateList() {
        return templateList;
    }

    public void setTemplateList(HashMap<FingerprintID, FingerprintTemplate> templateList) {
        this.templateList = templateList;
    }

    public FingerprintMatchingHandler(Activity mActivity) {
        this.mActivity = mActivity;
        this.isInitialized = false;
        this.templateList = new HashMap<>();
        this.mFPMatcher = new FingerprintMatcher();
    }

    public long verifyFingerPrint(FingerprintID nowID, byte[] nowImage, int nowWidth, int nowHeight, boolean[] matched) {

        long result = -1;
        boolean isError = false;
        Throwable errorObject = null;
        FingerprintTemplate nowFPTemplate = null;

        if (matched == null) return result;

        matched[0] = false;

        try {
            Log.d(TAG, "Preparing template");

            // Create the fingerprint template from the given image data
            nowFPTemplate = new FingerprintTemplate();
            nowFPTemplate.dpi(500).create(nowImage, nowWidth, nowHeight);

            Log.d(TAG, "Height: "+ nowHeight + " Weifth: " + nowWidth);

            // Initialize the fingerprint matcher if it is not already initialized
            if (mFPMatcher == null) {
                mFPMatcher = new FingerprintMatcher();
            }

            // Iterate through the entire template list and match each template
            boolean matchFound = false;  // Flag to track if a match is found
            double highestMatch = 0; // Variable to store the highest match score
            FingerprintID matchedID = null;  // Store the ID of the matched template

            if (!templateList.isEmpty()) {
                Iterator<Map.Entry<FingerprintID, FingerprintTemplate>> nowIterator = templateList.entrySet().iterator();
                while (nowIterator.hasNext()) {
                    Map.Entry<FingerprintID, FingerprintTemplate> mapElement = nowIterator.next();

                    // Skip the current fingerprint if its ID matches the nowID (to avoid comparing the same template)
                    if (mapElement.getKey() == nowID) {
                        continue;
                    }

                    // Get the existing template and index it
                    FingerprintTemplate existingTemplate = mapElement.getValue();
                    mFPMatcher.index(existingTemplate);

                    // Perform the matching
                    double matchScore = mFPMatcher.match(nowFPTemplate);
                    Log.d(TAG, "Match score with ID " + mapElement.getKey() + ": " + matchScore);

                    // Check if the match score exceeds the threshold (20 in this case)
                    if (matchScore > highestMatch) {
                        highestMatch = matchScore;
                        matchedID = mapElement.getKey();
                    }

                    // Set the flag if a match is found
                    if (highestMatch > 20) {
                        matchFound = true;
                        break; // Exit loop early if a valid match is found
                    }
                }
            }

            // If a match is found, update the matched array
            matched[0] = matchFound;

            // Log the highest match score
            Log.d(TAG, "Highest match score: " + highestMatch);

            // If no match is found, add the new template to the template list
            if (!matchFound) {
                templateList.put(nowID, nowFPTemplate);
            }

            // Log the size of the template list
            Log.d(TAG, "Template list size is " + ((templateList != null) ? templateList.size() : 0));

            result = 0;

        } catch (Throwable t) {
            isError = true;
            errorObject = t;

        } finally {
            if (isError) {
                Log.e(TAG, "Verify Fingerprint Error: " + errorObject.getMessage());
                errorObject.printStackTrace();
                result = -1;
                errorObject = null;
            }
            nowFPTemplate = null;
        }

        if (BuildConfig.isDebug) {
            Log.d(TAG, "Leaving verifyFingerprint");
        }
        return result;
    }

    public void verifyFingerPrint2(Integer fingerprintId, FingerprintTemplate searchTemplate,
                                   List<FingerprintTemplate> referenceFingerTemplateList,
                                   List<MatchResult> results) {
        Log.d(TAG, "Entered verifyFingerPrint2");



        // Clear and initialize results
        if (results == null) {
            results = new ArrayList<>();
        } else {
            results.clear();
        }

        // Check for null or empty inputs
        if (searchTemplate == null || referenceFingerTemplateList == null || referenceFingerTemplateList.isEmpty()) {
            Log.d(TAG, "Null or empty inputs detected");
            if (referenceFingerTemplateList == null || referenceFingerTemplateList.isEmpty())
            {
                Log.d(TAG, "Null or empty inputs detected referenceFingerTemplateList");
            }
            return;
        }

        try {
            Log.d(TAG, "Starting matching process");
            FingerprintMatcher matcher = new FingerprintMatcher();
            matcher.index(searchTemplate);  // Index the search template

            Log.d(TAG, "Fetching reference templates");
            for (FingerprintTemplate currentTemplate : referenceFingerTemplateList) {
                if (currentTemplate != null) {
                    // Perform matching and set the result
                    double matchScore = matcher.match(currentTemplate);
                    int intScore = (int) Math.round(matchScore);
                    Log.d(TAG, "match scorre: "+ intScore);


                    // Only add result if match score exceeds the threshold
                    if (matchScore >= MATCH_THRESHOULD) {
                        MatchResult result = new MatchResult();
                        result.setId(fingerprintId); // Set the provided fingerprint ID
                        result.setMatchScore(intScore); // Set the calculated match score
                        results.add(result);
                        Log.d(TAG, "Match score for template with ID " + FingerprintID.getFingerprintID(fingerprintId) + " is high threshold: " + matchScore);
                    } else {
                        Log.d(TAG, "Match score for template with ID " + FingerprintID.getFingerprintID(fingerprintId) + " is below threshold: " + matchScore);
                    }
                } else {
                    Log.d(TAG, "Skipping null reference template");
                }
            }
            Log.d(TAG, "Matching completed");

        } catch (Exception e) {
            Log.e(TAG, "Error in verifyFingerPrint2: " + e.getMessage(), e);
            results.clear(); // Clear results on error
        } finally {
            if (BuildConfig.isDebug) {
                Log.d(TAG, "Leaving verifyFingerPrint2");
            }
        }
    }

}
