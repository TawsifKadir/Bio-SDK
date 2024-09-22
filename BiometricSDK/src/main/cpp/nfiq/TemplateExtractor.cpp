
 // Include the header file that defines fjfx_create_fmd_from_raw and fjfx_get_pid
#include <cstring> // For memcpy
#include "TemplateExtractor.h"


 extern "C" JNIEXPORT int JNICALL Java_com_kit_fingerprintcapture_template_TemplateExtractor_createFmdFromRaw
(JNIEnv* env, jobject obj, jbyteArray rawImage, jint resolutionDpi, jint height, jint width, jint outputFormat, jbyteArray fmd, jintArray sizeOfFmd) {
    // Get pointers to the Java array data
    jbyte* rawImagePtr = env->GetByteArrayElements(rawImage, NULL);
    jbyte* fmdPtr = env->GetByteArrayElements(fmd, NULL);
    jint* sizeOfFmdPtr = env->GetIntArrayElements(sizeOfFmd, NULL);

    // Call the fjfx_create_fmd_from_raw function
    int result = fjfx_create_fmd_from_raw(
        rawImagePtr,
        static_cast<unsigned short>(resolutionDpi),
        static_cast<unsigned short>(height),
        static_cast<unsigned short>(width),
        static_cast<unsigned int>(outputFormat),
        fmdPtr,
        reinterpret_cast<unsigned int*>(sizeOfFmdPtr)
    );

    // Release the Java array data
    env->ReleaseByteArrayElements(rawImage, rawImagePtr, 0);
    env->ReleaseByteArrayElements(fmd, fmdPtr, 0);
    env->ReleaseIntArrayElements(sizeOfFmd, sizeOfFmdPtr, 0);

    return result;
}
