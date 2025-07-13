
package com.kit.fingerprintcapture.handlers;


import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintCacheEntry;

import com.machinezoo.sourceafis.FingerprintMatcher;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.List;

public class FingerPrintMatchChecker {
    private static final String TAG = "FingerprintCacheMatcher";




public boolean matchRawAgainstEnumerator2(byte[] rawImage, int width, int height) {
    if (rawImage == null || rawImage.length == 0) {
        Log.e(TAG, "Input raw image is null or empty");
        return false;
    }

    List<FingerprintCacheEntry> cachedList = FingerprintCache.getInstance().getFingerList();
    if (cachedList == null || cachedList.isEmpty()) {
        Log.d(TAG, "Fingerprint cache is empty");
        return false;
    }

    boolean isMatched = false;

    try {
        Log.d(TAG, "Preparing input template");
        FingerprintTemplate inputTemplate = new FingerprintTemplate().dpi(500).create(rawImage, width, height);
        FingerprintMatcher matcher = new FingerprintMatcher().index(inputTemplate);

        Log.d(TAG, "Performing matching with cached templates");
        for (FingerprintCacheEntry cacheEntry : cachedList) {
            if (cacheEntry == null || cacheEntry.getRawTemplate() == null) {
                Log.d(TAG, "Skipping cached entry with null template");
                continue;
            }

            try {
                double score = matcher.match(cacheEntry.getRawTemplate());

                Log.d(TAG, "Matched with ID: "
                        + (cacheEntry.getFingerprintId() != null ? cacheEntry.getFingerprintId().getName() : "Unknown")
                        + " | Score: " + score);

                if (score > 40) {
                    Log.d(TAG, "Fingerprint matched with cache entry.");
                    isMatched = true;
                    break;
                }

            } catch (Exception e) {
                Log.e(TAG, "Error matching with cached fingerprint: " + e.getMessage());
            }
        }

    } catch (Exception e) {
        Log.e(TAG, "Error preparing input template: " + e.getMessage());
    }

    if (!isMatched) {
        Log.d(TAG, "No matching fingerprint found in cache.");
    }

    return isMatched;
}



}
