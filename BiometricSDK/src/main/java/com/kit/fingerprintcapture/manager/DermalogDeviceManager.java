package com.kit.fingerprintcapture.manager;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.usb.UsbDevice;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;


import com.dermalog.afis.fingercode3.Encoder;
import com.dermalog.afis.fingercode3.FC3Exception;
import com.dermalog.afis.fingercode3.Matcher;
import com.dermalog.afis.fingercode3.Template;
import com.dermalog.afis.imagecontainer.DICException;
import com.dermalog.afis.imagecontainer.Decoder;
import com.dermalog.afis.imagecontainer.EncoderWsq;
import com.dermalog.afis.imagecontainer.RawImage;
import com.dermalog.afis.nistqualitycheck.Functions;
import com.dermalog.afis.nistqualitycheck.NistQualityCheckException;
import com.dermalog.biometricpassportsdk.BiometricPassportException;
import com.dermalog.biometricpassportsdk.BiometricPassportSdkAndroid;
import com.dermalog.biometricpassportsdk.Device;
import com.dermalog.biometricpassportsdk.DeviceCallback;
import com.dermalog.biometricpassportsdk.enums.CaptureMode;
import com.dermalog.biometricpassportsdk.enums.DeviceId;
import com.dermalog.biometricpassportsdk.enums.FeatureId;
import com.dermalog.biometricpassportsdk.enums.StatusLedColor;
import com.dermalog.biometricpassportsdk.structs.Position;
import com.dermalog.biometricpassportsdk.usb.permission.DevicePermission;
import com.dermalog.biometricpassportsdk.utils.BitmapUtil;
import com.dermalog.biometricpassportsdk.wrapped.BitmapInfoHeaderData;
import com.dermalog.biometricpassportsdk.wrapped.DeviceCallbackEventArgument;
import com.dermalog.biometricpassportsdk.wrapped.DeviceFeature;
import com.dermalog.biometricpassportsdk.wrapped.DeviceInfo;
import com.dermalog.biometricpassportsdk.wrapped.arguments.EventArgument;
import com.dermalog.biometricpassportsdk.wrapped.arguments.FingerprintSegment;
import com.dermalog.biometricpassportsdk.wrapped.arguments.FingerprintSegmentationArgument;
import com.dermalog.biometricpassportsdk.wrapped.arguments.ImageArgument;
import com.dermalog.hardware.DeviceManager;
import com.dermalog.hardware.PowerManager;
import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.FingerprintCaptureActivity;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.idemia.peripherals.PeripheralsPowerInterface;
import com.kit.fingerprintcapture.utils.FingerprintQualityCalculator;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.kit.fingerprintcapture.utils.LoadingGifUtility;
import com.kit.fingerprintcapture.utils.PowerSwitcherTask;
import com.morpho.android.usb.USBManager;
import com.morpho.morphosmart.sdk.CallbackMask;
import com.morpho.morphosmart.sdk.CallbackMessage;
import com.morpho.morphosmart.sdk.Coder;
import com.morpho.morphosmart.sdk.CompressionAlgorithm;
import com.morpho.morphosmart.sdk.CustomInteger;
import com.morpho.morphosmart.sdk.DetectionMode;
import com.morpho.morphosmart.sdk.EnrollmentType;
import com.morpho.morphosmart.sdk.ErrorCodes;
import com.morpho.morphosmart.sdk.LatentDetection;
import com.morpho.morphosmart.sdk.MorphoDevice;
import com.morpho.morphosmart.sdk.MorphoImage;
import com.morpho.morphosmart.sdk.TemplateFVPType;
import com.morpho.morphosmart.sdk.TemplateList;
import com.morpho.morphosmart.sdk.TemplateType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

public class DermalogDeviceManager implements IDeviceManager{

    private static final int LEFT_SHIFT_AMOUNT = 20;

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

    //ImageContainer
    private Decoder icDecoder;

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

        int ret = 0;
        Log.d(TAG, "initDermalogDevice");

        com.dermalog.afis.imagecontainer.Android.SetContext(mainActivity);
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
            return com.dermalog.common.exception.ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        deviceIsSet = true;
        return ErrorCodes.MORPHO_OK;
    }

    @Override
    public long openDevice() {
        Log.d(TAG, "openDevice() called");
        if(!deviceIsSet){
            long ret = initDevice();
            if(ret!=ErrorCodes.MORPHO_OK) return ret;
        }
        return ErrorCodes.MORPHO_OK;
    }

    @Override
    public long startCapture() {
        long ret = com.dermalog.common.exception.ErrorCodes.FPC_SUCCESS;

        try{
            ret = startFingerCapture();
        } catch (BiometricPassportException e) {
            Log.d(TAG, "startCapture() called but failed" + e.getMessage());
        }

        return ret;
    }

    @Override
    public long closeDevice() {
        long ret = ErrorCodes.MORPHO_OK;
        try {
            closeScanner();
        }catch(Exception exc){
            ret = com.dermalog.common.exception.ErrorCodes.FPC_ERROR_EMPTY_HANDLE;
            Log.e(TAG,"In CloseDevice error : "+exc.getMessage());
        }
        deviceIsSet = false;
        return ret;
    }

    @Override
    public long deInitDevice() {
        long ret = ErrorCodes.MORPHO_OK;
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

    void initializeSDKs() throws BiometricPassportException, DICException {
        Log.d(TAG, "initializeSDKs() called");
        biometricsSdk = new BiometricPassportSdkAndroid(mainActivity);

        icDecoder = new Decoder();
//        icWsqEncoder = new EncoderWsq();

        try {
            fc3Encoder = new Encoder();
            fc3Encoder.setCodingType(1); //fast encoding
//            fc3Matcher = new Matcher();
        } catch (FC3Exception e) {
            e.printStackTrace();
            Toast.makeText(mainActivity, "FingerCode3: NO LICENSE", Toast.LENGTH_LONG).show();
//            txtScore.setText("FC3\nN/A");
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

        if (icDecoder != null) {
            try {
                icDecoder.close();
            } catch (DICException e) {
                e.printStackTrace();
            }
            icDecoder = null;
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
//                    showWarning("No scanner attached!");
                    break;
                case NoPermission:
                    Log.d(TAG, "getPermissions() called noPermission");
//                    showError("No USB permission!");
                    break;
                case PartialPermission:
                    tryOpen = true;
                    Log.d(TAG, "getPermissions() called partialPermission");
//                    showWarning("Not all USB permissions!");
                    break;
                case Success:
                    Log.d(TAG, "getPermissions() called success");
                    tryOpen = true;
//                    showHint("Press 'CAPTURE'");
                    break;
                case UsbNotSupported:
                    Log.d(TAG, "getPermissions() called usbNotSupported");
//                    showError("No USB support!");
                    break;
            }

            if (tryOpen) {
                try {
                    openScanner();
                } catch (Exception e) {
                    Toast.makeText(mainActivity, "Error opening scanner", Toast.LENGTH_LONG).show();
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
                    scannerHandle.setFeature(FeatureId.CAPTURE_MODE, CaptureMode.FINGER_AUTO_DETECT_EMBEDDED.getValue());
                    break;
            }

        }

        scannerHandle.registerCallback(new DeviceCallback() {
            @Override
            public void onCall(Device device, DeviceCallbackEventArgument deviceCallbackEventArgument) {

                ImageArgument imageArgument = null;
                //Get detect image
                for (EventArgument ea : deviceCallbackEventArgument.getArguments()) {
                    if (ea instanceof ImageArgument) {
                        imageArgument = (ImageArgument) ea;
                    }
                }

                switch (deviceCallbackEventArgument.getEventId()){
                    case FINGER_DETECT:
                        capturing = false;
                        Log.d(TAG, "FINGER DETECT");
                        processImage(imageArgument,0);
                        break;
                    default:
                        Log.d(TAG, "NO FINGER DETECT");
                        if (imageArgument != null){
                            try {
                                Bitmap bmp = BitmapUtil.fromImageArgument(imageArgument);
                                if (bmp != null){
                                    Log.d(TAG, "onFingerprintPreview going with bitmap " + bmp);
                                    deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else {
                            Bitmap bmp = ImageProc.createPlaceholderBitmap(400,250,"Placeholder");
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
            return com.dermalog.common.exception.ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        if (scannerHandle == null) {
            showToastMessage("Scanner not opened",Toast.LENGTH_LONG);
            return com.dermalog.common.exception.ErrorCodes.FPC_ERROR_NOT_INITIALIZED;
        }

        capturing = true;
        setStatusLed(StatusLedColor.GREEN);
        scannerHandle.startCapture();
        return com.dermalog.common.exception.ErrorCodes.FPC_SUCCESS;
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

    void switchPowerOn() {
        PowerManager powerManager = DeviceManager.getDevice(mainActivity).getPowerManager();

        if (powerManager != null && powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_FINGERPRINT_SCANNER)) {
            powerManager.open();

            //Make sure to disable ADB via USB
            if(powerManager.isPowerTypeSupported(PowerManager.PowerType.USB_ADB_MODE)){
                powerManager.power(PowerManager.PowerType.USB_ADB_MODE, false);
            }

            LocalPowerSwitcherTask task = new LocalPowerSwitcherTask(mainActivity, PowerManager.PowerType.USB_FINGERPRINT_SCANNER);
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, true);
        }
    }

    class LocalPowerSwitcherTask extends PowerSwitcherTask {
        LocalPowerSwitcherTask(Context context, PowerManager.PowerType powerType) {
            super(context, powerType);
        }

        @Override
        protected void onPreExecute() {
//            showProgressDialog("Switching on USB power");
        }

        @Override
        protected void onPostExecute(Boolean result) {
//            hideProgressDialog();
            try {
                initializeSDKs();
                getPermissions();

            } catch (Exception e) {
                e.printStackTrace();
//                showError("Error creating SDKs: " + e.getMessage());
            }
        }
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

        private void processSingleImage() throws IOException, DICException {
            Bitmap bmp = BitmapUtil.fromImageArgument(imageArgument);
            RawImage rawImg = null;
            BitmapInfoHeaderData fingerImage = imageArgument.bitmapInfoHeaderData();
            try {
                Log.d(TAG, "processSingleImage() called width " + fingerImage.getWidth());
                Log.d(TAG, "processSingleImage() called height" + fingerImage.getHeight());
                rawImg = icDecoder.Decode(fingerImage.getRawData());
                Log.d(TAG, "processSingleImage() called image width " + rawImg.getPixelWidth());
                Log.d(TAG, "processSingleImage() called image height " + rawImg.getPixelHeight());
                deviceDataConsumer.onFingerprintData(fingerImage.getRawData(), fingerImage.getWidth(), fingerImage.getHeight(), (int) (score * 100), com.dermalog.common.exception.ErrorCodes.FPC_SUCCESS);

            } finally {
                if (rawImg != null) {
                    try {
                        rawImg.close();
                    } catch (DICException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
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

    private void showToastMessage(String msg, int length){
        Toast toast = Toast.makeText(mainActivity, msg, length);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 180);
        toast.show();
    }

}
