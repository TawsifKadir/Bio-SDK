package com.kit.fingerprintcapture.utils;

import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.machinezoo.sourceafis.FingerprintTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FingerprintsManager {

    public static String TAG = "FingerprintsManager";
    private Map<Integer, FingerprintTemplate> takenFingersTemplates =  new HashMap<>();
    private Map<Integer, FingerprintTemplate> enumeratorTemplates =  new HashMap<>();

    private List<FingerprintData> takenFingers = new ArrayList<>();
    private List<FingerprintData> enumeratorFingers = new ArrayList<>();
    final static int  nowWidth = 256, nowHeight = 400;
    public FingerprintsManager() {
        Log.d("FingerprintsManager", "Initializing FingerprintsManager...");
        // Load takenFingers from cache and build taken FingerprintTemplates
        FingerprintCache cache = FingerprintCache.getInstance();
        if (cache != null && cache.getFingerList() != null) {
            this.enumeratorFingers = new ArrayList<>(cache.getFingerList() != null
                    ? cache.getFingerList()
                    : new ArrayList<>());
            Log.d("FingerprintsManager", "Loaded enumeratorFingers from cache. Count: " + enumeratorFingers.size());
            for(FingerprintData fd: enumeratorFingers)
            {
                Log.d("FingerprintsManager", "Finger: \n" + fd.toString());
            }
            // Build enumerator FingerprintTemplates
            buildEnumeratorFingerprintTemplates();
            Log.d("FingerprintsManager", "Built enumeratorFingerprintTemplates. Count: " + enumeratorTemplates.size());
        } else {
            Log.d("FingerprintsManager", "No data in cache for takenFingers.");
        }

    }

    public Map<Integer, FingerprintTemplate> getTakenFingersTemplates() {
        return takenFingersTemplates;
    }

    public void setTakenFingersTemplates(Map<Integer, FingerprintTemplate> takenFingersTemplates) {
        this.takenFingersTemplates = takenFingersTemplates;
    }

    public Map<Integer, FingerprintTemplate> getEnumeratorTemplates() {
        return enumeratorTemplates;
    }

    public void setEnumeratorTemplates(Map<Integer, FingerprintTemplate> enumeratorTemplates) {
        this.enumeratorTemplates = enumeratorTemplates;
    }


    // Append methods
    public void appendTakenFingerFingerTemplete(FingerprintID fingerId, FingerprintTemplate template) {
        if (fingerId != null && template != null) {
            takenFingersTemplates.put(fingerId.getID(), template);
        }
    }


    public void appendEnumeratorTemplate(FingerprintID fingerId, FingerprintTemplate template) {
        if (fingerId != null && template != null) {
            enumeratorTemplates.put(fingerId.getID(), template);
        }
    }

    // Remove methods
    public void removeTakenFingerFromFingerprintTemplate(FingerprintID fingerId) {
        if (fingerId != null) {
            if (takenFingersTemplates.containsKey(fingerId.getID())) {
                takenFingersTemplates.remove(fingerId.getID());
                Log.d("FingerprintsManager", "Removed taken finger template with ID: " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in taken templates: " + fingerId.getName());
            }
        }
    }

    public void removeEnumeratorTemplate(FingerprintID fingerId) {
        if (fingerId != null) {
            if (enumeratorTemplates.containsKey(fingerId.getID())) {
                enumeratorTemplates.remove(fingerId.getID());
                Log.d("FingerprintsManager", "Removed enumerator finger template with ID: " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in enumerator templates: " + fingerId.getName());
            }
        }
    }


    public List<FingerprintData> getTakenFingers() {
        return takenFingers;
    }

    public void setTakenFingers(List<FingerprintData> takenFingers) {
        this.takenFingers = takenFingers;
    }

    public List<FingerprintData> getEnumeratorFingers() {
        return enumeratorFingers;
    }

    public void setEnumeratorFingers(List<FingerprintData> enumeratorFingers) {
        this.enumeratorFingers = enumeratorFingers;
    }




    public void buildTakenFingersFingerprintTemplates() {
        takenFingersTemplates.clear();
        for (FingerprintData data : takenFingers) {
            if (data != null && data.getFingerprintId() != null && data.getFingerprintData() != null) {
                FingerprintTemplate fingerprintTemplate = new FingerprintTemplate();
                fingerprintTemplate.dpi(500).create(ImageProc.fromWSQ(data.getFingerprintData(), nowWidth, nowHeight));
                takenFingersTemplates.put(data.getFingerprintId().getID(), fingerprintTemplate);
            }
        }
    }


    public void buildEnumeratorFingerprintTemplates() {
        enumeratorTemplates.clear();
        for (FingerprintData data : enumeratorFingers) {
            if (data != null && data.getFingerprintId() != null && data.getFingerprintData() != null) {
                FingerprintTemplate fingerprintTemplate = new FingerprintTemplate();
                if(data.getFingerprintData() != null)
                {
                    Log.d(TAG, "data size: " + data.getFingerprintData().length);
                    byte[] img = data.getFingerprintData();

                    byte[] decodedImage = ImageProc.fromWSQ(img, nowWidth, nowHeight);
                    if (decodedImage == null) {
                        Log.e(TAG, "WSQ decode failed for finger ID: " + data.getFingerprintId().getID());
                        continue;
                    }

// Build template (unchanged)
                    try {
                        fingerprintTemplate.dpi(500).create(decodedImage, nowWidth, nowHeight);
                        enumeratorTemplates.put(data.getFingerprintId().getID(), fingerprintTemplate);
                        Log.d(TAG, "Template built for finger ID: " + data.getFingerprintId().getID());
                    } catch (Exception e) {
                        Log.d(TAG, "Template data " + fingerprintTemplate);
                        Log.d(TAG, "FingerprintData id: " + data.getFingerprintId().getID());
                        Log.e(TAG, "Error creating fingerprint template for ID: "
                                + data.getFingerprintId().getID(), e);
                    }

                 //   enumeratorTemplates.put(data.getFingerprintId().getID(), fingerprintTemplate);


                }
                else {
                    Log.d(TAG, "data is null ");

                }

            }
        }
    }

    public void loadFromCacheInstance() {
        FingerprintCache cache = FingerprintCache.getInstance();
        if (cache != null && cache.getFingerList() != null) {
            this.takenFingers = new ArrayList<>(cache.getFingerList());
            buildTakenFingersFingerprintTemplates();
        }
    }




    // Append to takenFingers list
    public void appendTakenFingerData(FingerprintData data) {
        if (data != null && data.getFingerprintId() != null) {
            takenFingers.add(data);
            Log.d("FingerprintsManager", "Appended to takenFingers: ID = " + data.getFingerprintId().getName());
        } else {
            Log.w("FingerprintsManager", "Cannot append null or invalid FingerprintData to takenFingers.");
        }
    }

    // Append to enumeratorFingers list
    public void appendEnumeratorFingerData(FingerprintData data) {
        if (data != null && data.getFingerprintId() != null) {
            enumeratorFingers.add(data);
            Log.d("FingerprintsManager", "Appended to enumaratorFingers: ID = " + data.getFingerprintId().getName());
        } else {
            Log.w("FingerprintsManager", "Cannot append null or invalid FingerprintData to enumaratorFingers.");
        }
    }

    // Remove from takenFingers list
    public void removeTakenFingerData(FingerprintID fingerId) {
        if (fingerId != null) {
            boolean removed = false;
            Iterator<FingerprintData> iterator = takenFingers.iterator();
            while (iterator.hasNext()) {
                FingerprintData data = iterator.next();
                if (data != null && fingerId.getID() == data.getFingerprintId().getID()) {
                    iterator.remove(); // Removes the matching data
                    removed = true;
                    break; // Exit loop once the item is removed
                }
            }

            if (removed) {
                Log.d("FingerprintsManager", "Removed from takenFingers: ID = " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in takenFingers: " + fingerId.getName());
            }
        }
    }

    // Remove from enumeratorFingers list
    public void removeEnumeratorFingerData(FingerprintID fingerId) {
        if (fingerId != null) {
            boolean removed = false;
            Iterator<FingerprintData> iterator = enumeratorFingers.iterator();
            while (iterator.hasNext()) {
                FingerprintData data = iterator.next();
                if (data != null && fingerId.getID() == data.getFingerprintId().getID()) {
                    iterator.remove(); // Removes the matching data
                    removed = true;
                    break; // Exit loop once the item is removed
                }
            }

            if (removed) {
                Log.d("FingerprintsManager", "Removed from enumeratorFingers: ID = " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in enumeratorFingers: " + fingerId.getName());
            }
        }
    }



}
