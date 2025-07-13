package com.kit.fingerprintcapture.utils;

import android.content.Intent;
import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BiometricHelper {

    private static final String TAG = "BiometricHelper";

    public static List<FingerprintData> fingerIntentToFingerList(Intent intent) {
        if (intent == null) {
            Log.w(TAG, "Intent is null");
            return new ArrayList<>(); // Return empty list for safety
        }

        List<FingerprintData> fingerList = new ArrayList<>();

        for (FingerprintID fingerprintID : FingerprintID.values()) {
            try {
                FingerprintData fingerprintData = intent.getParcelableExtra(fingerprintID.getName());

                if (fingerprintData == null) {
                    Log.d(TAG, "No fingerprint data for: " + fingerprintID.getName());
                } else {
                    fingerList.add(fingerprintData);
                    Log.d(TAG, "Added fingerprint data for: " + fingerprintID.getName());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error processing fingerprint ID: " + fingerprintID.getName(), e);
            }
        }

        return fingerList;
    }

}
