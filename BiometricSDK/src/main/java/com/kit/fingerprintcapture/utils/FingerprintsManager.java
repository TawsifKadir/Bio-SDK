package com.kit.fingerprintcapture.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.localafis.sourceafis.FingerprintTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FingerprintsManager {

    public static String TAG = "FingerprintsManager";
    private Map<Integer, FingerprintTemplate> takenFingersTemplates =  new HashMap<>();
    private Map<Integer, FingerprintTemplate> enumeratorTemplates =  new HashMap<>();
    private List<FingerprintData> takenFingers = new ArrayList<>();
    private List<FingerprintData> enumeratorFingers = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
    );
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

        List<Future<?>> futures = new ArrayList<>();

        for (FingerprintData data : enumeratorFingers) {
            if (data == null || data.getFingerprintId() == null || data.getFingerprintData() == null) {
                Log.w(TAG, "⚠️ Missing fingerprint data, skipping entry");
                continue;
            }

            futures.add(executor.submit(() -> {
                int fingerId = data.getFingerprintId().getID();
                byte[] imgBytes = data.getFingerprintData();
                Log.d(TAG, "Image bytes " + Arrays.toString(Arrays.copyOf(imgBytes, 10)));
                Log.d(TAG, "Processing fingerprint ID=" + fingerId + " | size=" + imgBytes.length);

                try {
                    // --- Detect file type ---
                    String format = detectFormat(imgBytes);
                    Log.d(TAG, "Detected format: " + format);

                    ImageProc.DecodedImage decoded = null;

                    if ("JPEG".equals(format) || "PNG".equals(format)) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                        if (bitmap == null) {
                            Log.w(TAG, "❌ Failed to decode " + format + " image for finger ID=" + fingerId);
                            return;
                        }
                        decoded = toDecodedImage(bitmap);

                    } else if ("WSQ".equals(format)) {
                        decoded = ImageProc.fromWSQ(imgBytes);
                        if (decoded == null) {
                            Log.w(TAG, "❌ Failed to decode WSQ image for finger ID=" + fingerId);
                            return;
                        }

                    } else {
                        Log.w(TAG, "❌ Unknown/unsupported format for finger ID=" + fingerId);
                        return;
                    }

                    if (decoded == null || decoded.pixels == null) {
                        Log.w(TAG, "❌ Decoded image is null for finger ID=" + fingerId);
                        return;
                    }

                    FingerprintTemplate template = new FingerprintTemplate()
                            .dpi(500)
                            .create(decoded.pixels, decoded.width, decoded.height);

                    // Save safely in map (concurrent access!)
                    synchronized (enumeratorTemplates) {
                        enumeratorTemplates.put(fingerId, template);
                    }

                    Log.d(TAG, "✅ Template built for ID=" + fingerId +
                            " (w=" + decoded.width + ", h=" + decoded.height + ")");

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error creating template for finger ID=" + fingerId, e);
                }
            }));
        }

        // Optional: Wait until all templates are built before returning
        for (Future<?> f : futures) {
            try {
                f.get(); // blocks until finished
            } catch (Exception e) {
                Log.e(TAG, "⚠️ Error waiting for fingerprint template task", e);
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

    /**
     * Detects the image format from header bytes.
     */
    private String detectFormat(byte[] data) {
        if (data == null || data.length < 12) return "UNKNOWN";

        int b0 = data[0] & 0xFF;
        int b1 = data[1] & 0xFF;

        // JPEG
        if (b0 == 0xFF && b1 == 0xD8) return "JPEG";

        // PNG
        if (b0 == 0x89 && data[1] == 0x50 && data[2] == 0x4E && data[3] == 0x47)
            return "PNG";

        // WSQ (check SOI marker + "NIST" ASCII later)
        if (b0 == 0xFF && b1 == 0xA0) {
            // look ahead for "NIST"
            for (int i = 0; i < data.length - 4; i++) {
                if (data[i] == 'N' && data[i+1] == 'I' && data[i+2] == 'S' && data[i+3] == 'T') {
                    return "WSQ";
                }
            }
            return "WSQ"; // fallback if only header matches
        }

        return "UNKNOWN";
    }


    /**
     * Converts Bitmap → grayscale DecodedImage
     */
    private ImageProc.DecodedImage toDecodedImage(Bitmap bitmap) {
        if (bitmap == null) {
            return new ImageProc.DecodedImage(new byte[0], 0, 0, null);
        }

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int[] pixelsInt = new int[width * height];
        bitmap.getPixels(pixelsInt, 0, width, 0, 0, width, height);

        byte[] pixelsGray = new byte[width * height];
        for (int i = 0; i < pixelsInt.length; i++) {
            int color = pixelsInt[i];
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = (color) & 0xFF;
            pixelsGray[i] = (byte) ((r + g + b) / 3); // grayscale
        }

        // return with both raw grayscale pixels AND the original bitmap
        return new ImageProc.DecodedImage(pixelsGray, width, height, bitmap);
    }


}
