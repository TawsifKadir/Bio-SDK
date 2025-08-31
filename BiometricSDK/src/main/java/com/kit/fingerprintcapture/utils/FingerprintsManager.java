package com.kit.fingerprintcapture.utils;

import android.os.Build;
import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FingerprintsManager {


    private Map<Integer, ISOTemplate> takenFingersISOTemplates =  new HashMap<>();
    private Map<Integer, ISOTemplate> enumeratorISOTemplates =  new HashMap<>();
    private List<FingerprintData> takenFingers = new ArrayList<>();
    private List<FingerprintData> enumeratorFingers = new ArrayList<>();

    public FingerprintsManager() {
        Log.d("FingerprintsManager", "Initializing FingerprintsManager...");
        // Load takenFingers from cache and build taken ISOTemplates
        FingerprintCache cache = FingerprintCache.getInstance();
        if (cache != null && cache.getFingerList() != null) {
            this.enumeratorFingers = new ArrayList<>(cache.getFingerList() != null
                    ? cache.getFingerList()
                    : new ArrayList<>());
            Log.d("FingerprintsManager", "Loaded enumeratorFingers from cache. Count: " + enumeratorFingers.size());

            // Build enumerator ISOTemplates
            try {
                buildEnumeratorISOTemplates();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            Log.d("FingerprintsManager", "Built enumeratorISOTemplates. Count: " + enumeratorISOTemplates.size());
        } else {
            Log.d("FingerprintsManager", "No data in cache for takenFingers.");
        }

    }

    public Map<Integer, ISOTemplate> getTakenFingersISOTemplates() {
        return takenFingersISOTemplates;
    }

    public void setTakenFingersISOTemplates(Map<Integer, ISOTemplate> takenFingersISOTemplates) {
        this.takenFingersISOTemplates = takenFingersISOTemplates;
    }

    public Map<Integer, ISOTemplate> getEnumeratorISOTemplates() {
        return enumeratorISOTemplates;
    }

    public void setEnumeratorISOTemplates(Map<Integer, ISOTemplate> enumeratorISOTemplates) {
        this.enumeratorISOTemplates = enumeratorISOTemplates;
    }


    // Append methods
    public void appendTakenFingerIsoTemplete(FingerprintID fingerId, ISOTemplate template) {
        if (fingerId != null && template != null) {
            takenFingersISOTemplates.put(fingerId.getID(), template);
        }
    }


    public void appendEnumeratorTemplate(FingerprintID fingerId, ISOTemplate template) {
        if (fingerId != null && template != null) {
            enumeratorISOTemplates.put(fingerId.getID(), template);
        }
    }

    // Remove methods
    public void removeTakenFingerFromIsoTemplate(FingerprintID fingerId) {
        if (fingerId != null) {
            if (takenFingersISOTemplates.containsKey(fingerId.getID())) {
                takenFingersISOTemplates.remove(fingerId.getID());
                Log.d("FingerprintsManager", "Removed taken finger template with ID: " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in taken templates: " + fingerId.getName());
            }
        }
    }

    public void removeEnumeratorTemplate(FingerprintID fingerId) {
        if (fingerId != null) {
            if (enumeratorISOTemplates.containsKey(fingerId.getID())) {
                enumeratorISOTemplates.remove(fingerId.getID());
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




    public void buildTakenFingersISOTemplates() {
        takenFingersISOTemplates.clear();
        for (FingerprintData data : takenFingers) {
            if (data != null && data.getFingerprintId() != null && data.getIsoTemplate() != null) {
                ISOTemplate isoTemplate = new ISOTemplate(data.getIsoTemplate(), data.getIsoTemplate().length);
                takenFingersISOTemplates.put(data.getFingerprintId().getID(), isoTemplate);
            }
        }
    }


    public void buildEnumeratorISOTemplates() throws IOException {
        enumeratorISOTemplates.clear();
        for (FingerprintData data : enumeratorFingers) {
            if (data != null && data.getFingerprintId() != null && data.getIsoTemplate() != null) {
                ISOTemplate isoTemplate = new ISOTemplate(data.getIsoTemplate(), data.getIsoTemplate().length);
                ISOTemplate isoTemplate2005 = TemplateConverter.checkAndGetISO2005Version(isoTemplate);
                enumeratorISOTemplates.put(data.getFingerprintId().getID(), isoTemplate2005);
            }
        }
    }

    public void loadFromCacheInstance() {
        FingerprintCache cache = FingerprintCache.getInstance();
        if (cache != null && cache.getFingerList() != null) {
            this.takenFingers = new ArrayList<>(cache.getFingerList());
            buildTakenFingersISOTemplates();
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                removed = takenFingers.removeIf(data ->
                        data != null && fingerId.getID() == data.getFingerprintId().getID());
            }

            if (removed) {
                Log.d("FingerprintsManager", "Removed from takenFingers: ID = " + fingerId.getName());
            } else {
                Log.d("FingerprintsManager", "New finger detected, ID not found in takenFingers: " + fingerId.getName());
            }
        }
    }


}
