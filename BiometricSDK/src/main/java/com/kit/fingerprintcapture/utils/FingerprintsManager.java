package com.kit.fingerprintcapture.utils;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.localafis.sourceafis.FingerprintTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
            for(FingerprintData fd: enumeratorFingers) {
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

    public void appendTakenFingerFingerTempleteFromByteArray(FingerprintID fingerId, byte[] template) {
        if (fingerId != null && template != null) {
            FingerprintTemplate tempTemplate = new FingerprintTemplate();
            tempTemplate.convert(template);
            takenFingersTemplates.put(fingerId.getID(), tempTemplate);
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
                ImageProc.DecodedImage decodedImage = ImageProc.fromWSQ(data.getFingerprintData());
                fingerprintTemplate.dpi(500).create(decodedImage.pixels, decodedImage.width, decodedImage.height);
                takenFingersTemplates.put(data.getFingerprintId().getID(), fingerprintTemplate);
            }
        }
    }
    public void buildEnumeratorFingerprintTemplates() {
        enumeratorTemplates.clear();

        for (FingerprintData data : enumeratorFingers) {
            if (data != null && data.getFingerprintId() != null && data.getFingerprintData() != null) {
                try {
                    Log.d(TAG, "WSQ data size: " + data.getFingerprintData().length);
                    // Decode WSQ → grayscale + width/height
                    ImageProc.DecodedImage decoded = ImageProc.fromWSQ(data.getFingerprintData());

                    if (decoded == null || decoded.pixels == null) {
                        Log.w(TAG, "Decoded image is null for finger ID: " + data.getFingerprintId().getID());
                        continue;
                    }
                    // Build SourceAFIS template from decoded raw image
                    FingerprintTemplate fingerprintTemplate = new FingerprintTemplate();
                    fingerprintTemplate.dpi(500).create(decoded.pixels, decoded.width, decoded.height);
                    // Save in map
                    enumeratorTemplates.put(data.getFingerprintId().getID(), fingerprintTemplate);
                    Log.d(TAG, "Template built for finger ID: " + data.getFingerprintId().getID()
                            + " (w=" + decoded.width + ", h=" + decoded.height + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Error creating fingerprint template for ID: "
                            + data.getFingerprintId().getID(), e);
                }
            } else {
                Log.w(TAG, "FingerprintData missing for one enumerator");
            }
        }
    }

    public static void saveDecodedImageToFile(ImageProc.DecodedImage decoded, File file) throws IOException {
        if (decoded == null || decoded.pixels == null) {
            throw new IllegalArgumentException("Decoded image is null");
        }

        // Create Bitmap in 8-bit grayscale (use ARGB_8888 and map manually)
        Bitmap bitmap = Bitmap.createBitmap(decoded.width, decoded.height, Bitmap.Config.ARGB_8888);

        int[] pixelsARGB = new int[decoded.width * decoded.height];
        for (int i = 0; i < decoded.pixels.length; i++) {
            int grey = decoded.pixels[i] & 0xFF; // ensure unsigned
            pixelsARGB[i] = Color.rgb(grey, grey, grey);
        }
        bitmap.setPixels(pixelsARGB, 0, decoded.width, 0, 0, decoded.width, decoded.height);

        // Save bitmap as PNG
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
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
