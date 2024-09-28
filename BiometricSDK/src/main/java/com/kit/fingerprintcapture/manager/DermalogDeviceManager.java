package com.kit.fingerprintcapture.manager;

import android.app.Activity;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.dermalog.afis.fingercode3.Encoder;
import com.dermalog.afis.fingercode3.FC3Exception;
import com.dermalog.biometricpassportsdk.BiometricPassportException;
import com.dermalog.biometricpassportsdk.BiometricPassportSdkAndroid;
import com.dermalog.biometricpassportsdk.Device;
import com.dermalog.biometricpassportsdk.DeviceCallback;
import com.dermalog.biometricpassportsdk.enums.CaptureMode;
import com.dermalog.biometricpassportsdk.enums.DeviceId;
import com.dermalog.biometricpassportsdk.enums.FeatureId;
import com.dermalog.biometricpassportsdk.enums.StatusLedColor;
import com.dermalog.biometricpassportsdk.usb.permission.DevicePermission;
import com.dermalog.biometricpassportsdk.utils.BitmapUtil;
import com.dermalog.biometricpassportsdk.wrapped.BitmapInfoHeaderData;
import com.dermalog.biometricpassportsdk.wrapped.DeviceCallbackEventArgument;
import com.dermalog.biometricpassportsdk.wrapped.DeviceFeature;
import com.dermalog.biometricpassportsdk.wrapped.DeviceInfo;
import com.dermalog.biometricpassportsdk.wrapped.arguments.EventArgument;
import com.dermalog.biometricpassportsdk.wrapped.arguments.ImageArgument;
import com.dermalog.hardware.DeviceManager;
import com.dermalog.hardware.PowerManager;
import com.dermalog.common.exception.ErrorCodes;
import com.kit.BuildConfig;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.idemia.peripherals.PeripheralsPowerInterface;
import com.kit.fingerprintcapture.utils.ImageProc;

import java.io.IOException;
import java.util.List;

public class DermalogDeviceManager implements IDeviceManager{

    private String TAG = "DermalogDeviceManager";
    private DeviceDataCallback deviceDataConsumer;
    private Activity mainActivity;

    private ProcessImageTask imageTask;


    private boolean capturing = false;
    private boolean deviceIsSet = false;

    private byte[] mImageData;
    private int mImageWidth;
    private int mImageHeight;
    private int mQualityScore;

    private PeripheralsPowerInterface mPeripheralsInterface;

    //BiometricPassportSdk
    private BiometricPassportSdkAndroid biometricsSdk;
    private Device scannerHandle;

    private Encoder fc3Encoder;


    public DermalogDeviceManager(DeviceDataCallback deviceDataConsumer, Activity mainActivity) {
        this.deviceDataConsumer = deviceDataConsumer;
        this.mainActivity = mainActivity;

        mImageData=null;
        mImageWidth=0;
        mImageHeight=0;
        mQualityScore=0;
        capturing = false;
        deviceIsSet = false;
    }

    @Override
    public long initDevice() {

        Log.d(TAG, "initDermalogDevice");

        if (!BuildConfig.LICENSE.isEmpty()) {
            com.dermalog.afis.fingercode3.Android.SetLicense(BuildConfig.LICENSE.getBytes(), mainActivity.getApplicationContext());
        } else {
            com.dermalog.afis.fingercode3.Android.SetContext(mainActivity.getApplicationContext());
        }

        try{
            switchPowerOn();
        }catch (Exception e){
            deviceIsSet = false;
            Log.d(TAG, "initDevice() called failed");
            return ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        deviceIsSet = true;
        return ErrorCodes.FPC_SUCCESS;
    }

    @Override
    public long openDevice() {
        Log.d(TAG, "openDevice() called");
        if(!deviceIsSet){
            long ret = initDevice();
            if(ret!=ErrorCodes.FPC_SUCCESS) return ret;
        }
        return ErrorCodes.FPC_SUCCESS;
    }

    @Override
    public long startCapture() {
        long ret = ErrorCodes.FPC_SUCCESS;

        try{
            ret = startFingerCapture();
        } catch (BiometricPassportException e) {
            Log.d(TAG, "startCapture() called but failed" + e.getMessage());
        }

        return ret;
    }

    @Override
    public long closeDevice() {
        long ret = ErrorCodes.FPC_SUCCESS;
        try {
            closeScanner();
        }catch(Exception exc){
            ret = ErrorCodes.FPC_ERROR_EMPTY_HANDLE;
            Log.e(TAG,"In CloseDevice error : "+exc.getMessage());
        }
        deviceIsSet = false;
        return ret;
    }

    @Override
    public long deInitDevice() {
        long ret = ErrorCodes.FPC_SUCCESS;
        if(deviceIsSet){
            uninitializeSDK();
            deviceIsSet = false;
        }
        return ret;
    }

    @Override
    public boolean isDeviceOpen() {
        return deviceIsSet;
    }

    @Override
    public boolean isPermissionAcquired() {

        return true;
    }

    void initializeSDKs() throws BiometricPassportException {
        Log.d(TAG, "initializeSDKs() called");
        biometricsSdk = new BiometricPassportSdkAndroid(mainActivity);
        try {
            fc3Encoder = new Encoder();
            fc3Encoder.setCodingType(1); //fast encoding
            Log.d(TAG, "initializeSDKs() called License found");
        } catch (FC3Exception e) {
            Log.d(TAG, "initializeSDKs() called License not found");
            e.printStackTrace();
            Toast.makeText(mainActivity, "FingerCode3: NO LICENSE", Toast.LENGTH_LONG).show();
        }
    }

    private void uninitializeSDK() {
        closeScanner();

        if (biometricsSdk != null) {
            biometricsSdk.dispose();
            biometricsSdk = null;
        }

        if (fc3Encoder != null) {
            try {
                fc3Encoder.close();
            } catch (FC3Exception e) {
                e.printStackTrace();
            }
            fc3Encoder = null;
        }
    }

    void getPermissions() {
        Log.d(TAG, "getPermissions() called");
        biometricsSdk.requestUSBPermissionsAsync(permissionResult -> {
            for (DevicePermission dp : permissionResult.getDevicePermissions()) {
                UsbDevice d = dp.getDevice();
                Log.d(TAG, "Permission for USB device '" + d.getProductName()
                        + "', serial# '" + (dp.isGranted() ? "" : " not") + " granted");
            }

            boolean tryOpen = false;

            switch (permissionResult.getResult()) {
                case NoDevice:
                    Log.d(TAG, "getPermissions() called noDevice");
                    break;
                case NoPermission:
                    Log.d(TAG, "getPermissions() called noPermission");
                    break;
                case PartialPermission:
                    tryOpen = true;
                    Log.d(TAG, "getPermissions() called partialPermission");
                    break;
                case Success:
                    Log.d(TAG, "getPermissions() called success");
                    tryOpen = true;
                    break;
                case UsbNotSupported:
                    Log.d(TAG, "getPermissions() called usbNotSupported");
                    break;
            }

            if (tryOpen) {
                try {
                    openScanner();
                } catch (Exception e) {
                    Toast.makeText(mainActivity, "Error opening scanner", Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Could not open scanner " + e.getMessage());
                }
            }

        });
    }

    private void openScanner() throws BiometricPassportException {
        Log.d(TAG, "openScanner() called");
        closeScanner();

        // search scanner and open first
        DeviceInfo[] devices = biometricsSdk.enumerateDevices(DeviceId.ALL);
        if (devices.length == 0) {
//            showError("EnumerateDevices returned no device");
            Log.d(TAG, "openScanner() called no devices");
            showToastMessage("EnumerateDevices returned no device",Toast.LENGTH_LONG);
            return;
        }else{
            Log.d(TAG, "openScanner() called device found");
        }

        scannerHandle = biometricsSdk.createDevice(devices[0]);

        List<DeviceFeature> features = scannerHandle.getSupportedFeatures();

        for (DeviceFeature f : features) {

            switch (f.getId()) {
                case FINGER_MASK_SENSITIVITY:
                    //Change mask sensitivity. Value depends on used scanner.
                    //Suprema BioSlim 3: 1-7. default: 7
                    scannerHandle.setFeature(FeatureId.FINGER_MASK_SENSITIVITY, 7);
                    break;

                case FINGER_LOW_CONTRAST_MODE:
                    //Enable Low contrast mode for better dry finger detection (e.g. F1 / ZF1)
                    scannerHandle.setFeature(FeatureId.FINGER_LOW_CONTRAST_MODE, 1);
                    break;

                case CAPTURE_MODE:
                    scannerHandle.setFeature(FeatureId.CAPTURE_MODE, CaptureMode.FINGER_AUTO_DETECT.getValue());
                    break;
            }

        }

        scannerHandle.registerCallback(new DeviceCallback() {
            @Override
            public void onCall(Device device, DeviceCallbackEventArgument deviceCallbackEventArgument) {

                Bitmap bmp = null;
                ImageArgument imageArgument = null;
                //Get detect image
                for (EventArgument ea : deviceCallbackEventArgument.getArguments()) {
                    if (ea instanceof ImageArgument) {
                        imageArgument = (ImageArgument) ea;
                    }
                }

                switch (deviceCallbackEventArgument.getEventId()){
                    case START:
                        bmp = ImageProc.createEmptyBitmap(400,250);
                        deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
                        break;
                    case FINGER_DETECT:
                        capturing = false;
                        Log.d(TAG, "finger case: FINGER DETECT");
                        processImage(imageArgument,0);
                        break;
                    case FINGER_REMOVE:
                        Log.d(TAG, "finger case: FINGER REMOVE");
                        bmp = ImageProc.createEmptyBitmap(400,250);
                        deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
                        break;
                    case FINGER_IMAGE:
                        Log.d(TAG, "finger case: FINGER IMAGE");
                        if (imageArgument != null){
                            try {
                                bmp = BitmapUtil.fromImageArgument(imageArgument);
                                if (bmp != null){
                                    Log.d(TAG, "onFingerprintPreview going with bitmap " + bmp);
                                    deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else {
                            bmp = ImageProc.createEmptyBitmap(400,250);
                            deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
                        }
                        break;
                }

            }
        });

    }

    private void closeScanner() {
        if (scannerHandle != null) {
            scannerHandle.dispose();
            scannerHandle = null;
        }
    }

    private int startFingerCapture() throws BiometricPassportException {
        if (biometricsSdk == null) {
            showToastMessage("SDK is null",Toast.LENGTH_LONG);
            return ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        if (scannerHandle == null) {
            showToastMessage("Scanner not opened",Toast.LENGTH_LONG);
            return ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        capturing = true;
        setStatusLed(StatusLedColor.GREEN);
        scannerHandle.startCapture();
        return ErrorCodes.FPC_SUCCESS;
    }

    void processImage(final ImageArgument imageArgument, double score) {

        if (imageTask != null && imageTask.getStatus() != AsyncTask.Status.FINISHED)
            return;

        imageTask = new ProcessImageTask(imageArgument,score);
        imageTask.execute();
    }

    private void setStatusLed(StatusLedColor color) throws BiometricPassportException {
        if(scannerHandle.getDeviceId() == DeviceId.F1) {
            scannerHandle.setStatusLeds(new byte[]{(byte)color.ordinal()});
        }
    }

    void switchPowerOn() throws BiometricPassportException {
        PowerManager powerManager = DeviceManager.getDevice(mainActivity).getPowerManager();

        if (powerManager == null) {
            // Error code 1001 for PowerManager being unavailable
            throw new BiometricPassportException(ErrorCodes.FPC_ERROR_NO_HANDLE, "PowerManager is unavailable.");
        }

        // Check if the power type for USB Fingerprint Scanner is supported
        if (powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_FINGERPRINT_SCANNER)) {
            try {
                powerManager.open();

                // Ensure USB ADB mode is supported and disable it
                if (powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_ADB_MODE)) {
                    powerManager.power(PowerManager.PowerType.USB_ADB_MODE, false);
                }

                // Power on the USB Fingerprint Scanner
                powerManager.power(PowerManager.PowerType.USB_FINGERPRINT_SCANNER, true);

                // If everything succeeded, initialize SDKs and get permissions
                initializeSDKs();
                getPermissions();

            } catch (Exception e) {
                // Error code 1002 for failure to power on the fingerprint scanner
                e.printStackTrace();
                throw new BiometricPassportException(ErrorCodes.FPC_ERROR_DEVICE_AUTHORIZATION_CODE, "Failed to power on USB Fingerprint Scanner: " + e.getMessage());
            }
        } else {
            // Error code 1003 for unsupported USB Fingerprint Scanner
            throw new BiometricPassportException(ErrorCodes.FPC_ERROR_DEVICE_AUTHORIZATION_CODE, "USB Fingerprint Scanner is not supported on this device.");
        }
    }


    private void showToastMessage(String msg, int length){
        Toast toast = Toast.makeText(mainActivity, msg, length);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 180);
        toast.show();
    }

    class ProcessImageTask extends AsyncTask<Void, String, Exception> {

        ImageArgument imageArgument;
        double score;

        public ProcessImageTask(ImageArgument image,double score){
            this.imageArgument = image;
            this.score = score;
        }

        @Override
        protected void onPreExecute() {

        }

        private void processSingleImage(){
            BitmapInfoHeaderData fingerImage = imageArgument.bitmapInfoHeaderData();
            Log.d(TAG, "processSingleImage() called width " + fingerImage.getWidth());
            Log.d(TAG, "processSingleImage() called height" + fingerImage.getHeight());
            deviceDataConsumer.onFingerprintData(fingerImage.getRawData(), fingerImage.getWidth(), fingerImage.getHeight(), (int) (score * 100), com.dermalog.common.exception.ErrorCodes.FPC_SUCCESS);
        }

        @Override
        protected Exception doInBackground(Void... voids) {
            try {
                scannerHandle.stopCapture();
                setStatusLed(StatusLedColor.OFF);
            } catch (BiometricPassportException e) {
                e.printStackTrace();
                return e;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Exception e) {
            if (e == null){
                try{
                    processSingleImage();
                }catch (Exception exc){
                    exc.printStackTrace();
                }
            }else{
                e.printStackTrace();
            }
        }
    }

}
