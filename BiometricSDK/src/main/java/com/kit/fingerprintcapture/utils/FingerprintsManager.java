package com.kit.fingerprintcapture.utils;

import android.graphics.Bitmap;
import android.util.Log;

import com.kit.fingerprintcapture.model.FingerprintCache;
import com.kit.fingerprintcapture.model.FingerprintData;
import com.kit.fingerprintcapture.model.FingerprintID;
import com.kit.fingerprintcapture.template.ISOTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class FingerprintsManager {

    public static String TAG = "FingerprintsManager";
    private Map<Integer, ISOTemplate> takenFingersTemplates =  new HashMap<>();
    private Map<Integer, ISOTemplate> enumeratorTemplates =  new HashMap<>();
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
//            buildEnumeratorFingerprintTemplates();
            Log.d("FingerprintsManager", "Built enumeratorFingerprintTemplates. Count: " + enumeratorTemplates.size());
        } else {
            Log.d("FingerprintsManager", "No data in cache for takenFingers.");
        }
    }
    public Map<Integer, ISOTemplate> getTakenFingersTemplates() {
        return takenFingersTemplates;
    }

    public void setTakenFingersTemplates(Map<Integer, ISOTemplate> takenFingersTemplates) {
        this.takenFingersTemplates = takenFingersTemplates;
    }

    public Map<Integer, ISOTemplate> getEnumeratorTemplates() {
        return enumeratorTemplates;
    }
    public void setEnumeratorTemplates(Map<Integer, ISOTemplate> enumeratorTemplates) {
        this.enumeratorTemplates = enumeratorTemplates;
    }
    // Append methods
    public void appendTakenFingerFingerTemplete(FingerprintID fingerId, ISOTemplate template) {
        if (fingerId != null && template != null) {
            takenFingersTemplates.put(fingerId.getID(), template);
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
                ISOTemplate fingerprintTemplate = null;
                ImageProc.DecodedImage decodedImage = ImageProc.fromWSQ(data.getFingerprintData());
                try{
                    fingerprintTemplate = TemplateUtils.createISOTemplate(decodedImage.pixels, decodedImage.width, decodedImage.height);
                }catch (Exception e){
                    Log.d(TAG, "Error is " + e.getMessage());
                }
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

            int fingerId = data.getFingerprintId().getID();
            byte[] templateBytes = data.getIsoTemplate();
            ISOTemplate template = new ISOTemplate(templateBytes, templateBytes.length);
            enumeratorTemplates.put(fingerId, template);

            // Detect upfront
//            String format = detectFormat(imgBytes);

//            if ("WSQ".equals(format)) {
//                // Force WSQ decoding in the caller thread (sequential, avoids crash)
//                processWSQ(fingerId, imgBytes);
//            } else {
//                // Non-WSQ formats can run in parallel safely
//                futures.add(executor.submit(() -> processOtherFormats(fingerId, imgBytes, format)));
//            }
        }

//        // Wait for non-WSQ futures
//        for (Future<?> f : futures) {
//            try {
//                f.get();
//            } catch (Exception e) {
//                Log.e(TAG, "⚠️ Error waiting for fingerprint template task", e);
//            }
//        }
    }

//    private void processWSQ(int fingerId, byte[] imgBytes) {
//        try {
//            ImageProc.DecodedImage decoded = ImageProc.fromWSQ(imgBytes);
//
//            if (decoded == null || decoded.pixels == null || decoded.width <= 0 || decoded.height <= 0) {
//                Log.w(TAG, "❌ WSQ decode failed for finger ID=" + fingerId);
//                return;
//            }
//
//            FingerprintTemplate template = new FingerprintTemplate()
//                    .dpi(500)
//                    .create(decoded.pixels, decoded.width, decoded.height);
//
//            synchronized (enumeratorTemplates) {
//                enumeratorTemplates.put(fingerId, template);
//            }
//
//            Log.d(TAG, "✅ WSQ template built for ID=" + fingerId +
//                    " (w=" + decoded.width + ", h=" + decoded.height + ")");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error creating WSQ template for finger ID=" + fingerId, e);
//        }
//    }
//
//    private void processOtherFormats(int fingerId, byte[] imgBytes, String format) {
//        try {
//            ImageProc.DecodedImage decoded = null;
//
//            if ("JPEG".equals(format) || "PNG".equals(format)) {
//                Bitmap bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
//                if (bitmap == null) {
//                    Log.w(TAG, "❌ Failed to decode " + format + " image for finger ID=" + fingerId);
//                    return;
//                }
//                decoded = toDecodedImage(bitmap);
//            }
//
//            if (decoded == null || decoded.pixels == null || decoded.width <= 0 || decoded.height <= 0) {
//                Log.w(TAG, "❌ Decoded image invalid for finger ID=" + fingerId);
//                return;
//            }
//
//            FingerprintTemplate template = new FingerprintTemplate()
//                    .dpi(500)
//                    .create(decoded.pixels, decoded.width, decoded.height);
//
//            synchronized (enumeratorTemplates) {
//                enumeratorTemplates.put(fingerId, template);
//            }
//
//            Log.d(TAG, "✅ Template built for ID=" + fingerId +
//                    " (w=" + decoded.width + ", h=" + decoded.height + ")");
//
//        } catch (Exception e) {
//            Log.e(TAG, "❌ Error creating template for finger ID=" + fingerId, e);
//        }
//    }
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
