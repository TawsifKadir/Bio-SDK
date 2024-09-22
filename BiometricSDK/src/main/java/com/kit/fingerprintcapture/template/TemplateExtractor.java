package com.kit.fingerprintcapture.template;

import android.util.Log;

public class TemplateExtractor {

    public static int  FJFX_SUCCESS = 0;     // Extraction succeeded, minutiae data is in output buffer.
    public static int FJFX_FAIL_IMAGE_SIZE_NOT_SUP = 1;     // Failed. Input image size was too large or too small.
    public static int FJFX_FAIL_EXTRACTION_UNSPEC = 2;     // Failed. Unknown error.
    public static int FJFX_FAIL_EXTRACTION_BAD_IMP = 3;     // Failed. No fingerprint detected in input image.
    public static int FJFX_FAIL_INVALID_OUTPUT_FORMAT = 7;     // Failed. Invalid output record type - only ANSI INCIT 378-2004 or ISO/IEC 19794-2:2005 are supported.
    public static int FJFX_FAIL_OUTPUT_BUFFER_IS_TOO_SMALL = 8;     // Failed. Output buffer too small.

    public static int FJFX_FMD_ANSI_378_2004 = 0x001B0201;   // ANSI INCIT 378-2004 data format
    public static int FJFX_FMD_ISO_19794_2_2005 = 0x01010001;   // ISO/IEC 19794-2:2005 data format

    static {
        try {
            // Attempt to load the native library
            System.loadLibrary("templatelib");
            Log.d("Template Extractor","Library Loaded");
        } catch (Throwable e) {
            // Handle the exception if the library cannot be loaded
            e.printStackTrace();
            // Optionally, you can log the error or notify the user
            Log.e("MyNativeLibrary", "Failed to load native library: " + e.getMessage());
        }
    }

    private static TemplateExtractor myInstance = null;
    private static Object LOCK = new Object();

    public static TemplateExtractor getMyInstance(){

        synchronized (LOCK){
            if(myInstance==null){
                myInstance = new TemplateExtractor();
            }
        }
        return myInstance;
    }

    // Native method declaration
    public native int createFmdFromRaw(byte[] rawImage, int dpi, int height, int width, int outputFormat,byte[] fmd, int[] sizeOfFmdPtr);

}
