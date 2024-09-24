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

public class DermalogDeviceManager implements IDeviceManager,Observer{

    private static final int LEFT_SHIFT_AMOUNT = 20;

    private String TAG = "MorphoDeviceManager02";
    private DeviceDataCallback deviceDataConsumer;
    private Activity mainActivity;

    private ProcessImageTask imageTask;

    private MorphoDevice morphoDevice;

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

    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPeripheralsInterface = PeripheralsPowerInterface.Stub.asInterface(service);

            if(BuildConfig.isDebug) {
                Log.d(TAG, "aidl connect succes");
            }

            if (!getFingerprintSensorState()){
                android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(mainActivity).create();
                alertDialog.setCancelable(false);
                alertDialog.setTitle(R.string.app_name);
                alertDialog.setMessage(mainActivity.getString(R.string.noAccessToDevice));
                alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", (DialogInterface.OnClickListener) null);
                alertDialog.show();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mPeripheralsInterface = null;

            if(BuildConfig.isDebug) {
                Log.d(TAG, "aidl disconnected");
            }
        }
    };


    public DermalogDeviceManager(DeviceDataCallback deviceDataConsumer, Activity mainActivity) {
        this.deviceDataConsumer = deviceDataConsumer;
        this.mainActivity = mainActivity;

        morphoDevice=null;
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
        MorphoDevice md = null;
        Log.d(TAG, "initMorphoDevice");

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

        morphoDevice=md;
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
//        if (!capturing){
//            capturing = true;
//            ret = morphoDeviceCapture();
//        }
//        else if (capturing && deviceIsSet) {
//            ret = closeDevice();
//            capturing = false;
//            deviceIsSet = false;
//        }
//        else{
//            showToastMessage("Device is being initialized, please try again", Toast.LENGTH_SHORT);
//            ret = ErrorCodes.MORPHOERR_OTP_NOT_INITIALIZED;
//        }

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
//                    btnCaptureLeft.setEnabled(false);
//                    btnCaptureRight.setEnabled(false);
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
//                switch (deviceCallbackEventArgument.getEventId()) {
//                    case START:
//                       break;
//
//                    case ERROR:
//                    break;
//
//                    case FINGER_TOUCH:
//                        break;
//
//                    case FINGER_IMAGE:
////                    showHint("FINGER_IMAGE");
//                        if (imageArgument != null) {
//                            try {
//                                bmp = BitmapUtil.fromImageArgument(imageArgument);
////                            displayImage(bmp);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        if (bmp != null){
//                            deviceDataConsumer.onFingerprintPreview(bmp, bmp.getWidth(), bmp.getHeight());
//                        }
//                        break;
//
//                    case FINGER_DETECT:
////                    showHint("FINGER_DETECT");
////                    processImage(imageArgument, segments);
//                        byte[] imageData = null;
//                        if (imageArgument != null) {
//                            try {
//                                bmp = BitmapUtil.fromImageArgument(imageArgument);
//                                imageData = imageArgument.bitmapInfoHeaderData().getRawData();
////                            displayImage(bmp);
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//
//                        if (bmp != null && imageData != null){
//                            deviceDataConsumer.onFingerprintData(imageData, bmp.getWidth(), bmp.getHeight(), 0, com.dermalog.common.exception.ErrorCodes.FPC_SUCCESS);
//                        }
//                        break;
//
//                    case STOP:
//                        break;
//
//                }


            }
        });


//        runOnUiThread(() -> {
//            btnCaptureLeft.setEnabled(true);
//            btnCaptureRight.setEnabled(true);
//        });

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

    @Override
    public void update(Observable observable, Object data) {
        try
        {
            // convert the object to a callback back message.
            CallbackMessage message = (CallbackMessage) data;
            int type = message.getMessageType();

            String strMessage = "";
            switch (type)
            {

                case 1: {
                    // message is a command.
                    Integer command = (Integer) message.getMessage();

                    // Analyze the command.
                    switch (command) {
                        case 0:
                            strMessage = "No finger detected";
                            break;
                        case 1:
                            strMessage = "Move finger up";
                            break;
                        case 2:
                            strMessage = "Move finger down";
                            break;
                        case 3:
                            strMessage = "Move finger left";
                            break;
                        case 4:
                            strMessage = "Move finger right";
                            break;
                        case 5:
                            strMessage = "Press harder";
                            break;
                        case 6:
                            strMessage = "Remove finger";
                            break;
                        case 7:
                            strMessage = "Remove finger";
                            break;
                        case 8:
                            strMessage = "Finger detected";
                            break;
                    }
                    deviceDataConsumer.onCaptureCmd(strMessage);
                    break;
                }
                case 2: {
                    // message is a low resolution image, display it.
                    byte[] image = (byte[]) message.getMessage();

                    MorphoImage morphoImage = MorphoImage.getMorphoImageFromLive(image);
                    int imageRowNumber = morphoImage.getMorphoImageHeader().getNbRow();
                    int imageColumnNumber = morphoImage.getMorphoImageHeader().getNbColumn();
                    final Bitmap imageBmp = Bitmap.createBitmap(imageColumnNumber, imageRowNumber, Bitmap.Config.ALPHA_8);
                    ByteBuffer nowBuffer = ByteBuffer.wrap(morphoImage.getImage(), 0, morphoImage.getImage().length);

                    mImageWidth = imageColumnNumber;
                    mImageHeight = imageRowNumber;
                    mImageData = nowBuffer.array();
                    imageBmp.copyPixelsFromBuffer(nowBuffer);
                    deviceDataConsumer.onFingerprintPreview(imageBmp, imageBmp.getWidth(), imageBmp.getHeight());
                }
                break;
                case 3:
                    mQualityScore = (Integer)message.getMessage();
                    break;

            }

        }
        catch (Throwable e)
        {
            Log.e("ProcessObserver", "update : " + e.getMessage());

        }
    }

    /**************************** CAPTURE *********************************/
    public long morphoDeviceCapture() {

        if (morphoDevice == null){
            long ret = initDevice();
            if(ret!=ErrorCodes.MORPHO_OK) {
                return ret;
            }
        }
        /********* CAPTURE THREAD *************/
        Thread commandThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = 0;
                    int timeout = 30;
                    final int acquisitionThreshold = 0;
                    int advancedSecurityLevelsRequired = 0;
                    int fingerNumber = 1;

                    TemplateType templateType = TemplateType.MORPHO_PK_ISO_FMR;
                    TemplateFVPType templateFVPType = TemplateFVPType.MORPHO_NO_PK_FVP;
                    int maxSizeTemplate = 512;

                    EnrollmentType enrollType = EnrollmentType.ONE_ACQUISITIONS;
                    LatentDetection latentDetection = LatentDetection.LATENT_DETECT_ENABLE;

                    Coder coderChoice = Coder.MORPHO_DEFAULT_CODER;
                    int detectModeChoice = DetectionMode.MORPHO_ENROLL_DETECT_MODE.getValue()
                            | DetectionMode.MORPHO_FORCE_FINGER_ON_TOP_DETECT_MODE.getValue();//18;

                    TemplateList templateList = new TemplateList();

                    // Define the messages sent through the callback
                    int callbackCmd = CallbackMask.MORPHO_CALLBACK_COMMAND_CMD.getValue()
                            | CallbackMask.MORPHO_CALLBACK_IMAGE_CMD.getValue()
                            | CallbackMask.MORPHO_CALLBACK_CODEQUALITY.getValue()
                            | CallbackMask.MORPHO_CALLBACK_DETECTQUALITY.getValue();

                    MorphoImage nowImage = new MorphoImage();

                    /********* CAPTURE *************/

                    ret = morphoDevice.getImage(timeout, acquisitionThreshold, CompressionAlgorithm.MORPHO_NO_COMPRESS,
                            0, detectModeChoice, latentDetection, nowImage, callbackCmd,
                            DermalogDeviceManager.this);
                    if(BuildConfig.isDebug) {
                        Log.d(TAG, "morphoDeviceCapture ret = " + ret);
                    }

                    if (ret != ErrorCodes.MORPHO_OK) {
                        String err = "";
                        if (ret == ErrorCodes.MORPHOERR_TIMEOUT) {
                            err = "Capture failed : timeout";
                        } else if (ret == ErrorCodes.MORPHOERR_CMDE_ABORTED) {
                            err = "Capture aborted";
                        } else if (ret == ErrorCodes.MORPHOERR_UNAVAILABLE) {
                            err = "Device is not available";
                        } else {
                            err = "Error code is " + ret;
                        }

                        deviceDataConsumer.onCaptureError(err);

                    } else {

                        if(nowImage.getImage()!=null){
                            int imageRowNumber = nowImage.getMorphoImageHeader().getNbRow();
                            int imageColumnNumber = nowImage.getMorphoImageHeader().getNbColumn();
                            ByteBuffer nowBuffer = ByteBuffer.wrap(nowImage.getImage(), 0, nowImage.getImage().length);
                            mImageWidth = imageColumnNumber;
                            mImageHeight = imageRowNumber;
                            mImageData = nowBuffer.array();

                        }

                        deviceDataConsumer.onFingerprintData(mImageData,mImageWidth,mImageHeight,mQualityScore,ErrorCodes.MORPHO_OK);
                    }
                }catch(Exception exc){
                    Log.e(TAG, "morphoDeviceCapture Error ");
                }finally {
                    DermalogDeviceManager.this.capturing = false;
                }

            }
        });
        commandThread.start();

        return ErrorCodes.MORPHO_OK;
    }

    // Close the USB device
    public long closeMorphoDevice(){

        if(morphoDevice != null) {

            if(BuildConfig.isDebug) {
                Log.d(TAG, "closeMorphoDevice");
            }

            try {
                morphoDevice.cancelLiveAcquisition();
                morphoDevice.closeDevice();

            } catch (Exception e) {
                Log.e(TAG, "closeMorphoDevice : " + e.getMessage());
                return ErrorCodes.MORPHOERR_CLOSE_COM;
            }finally {
                morphoDevice = null;
            }
        }
        return ErrorCodes.MORPHO_OK;
    }
    private Intent getAidlIntent() {
        Intent aidlIntent = new Intent();
        aidlIntent.setAction("idemia.intent.action.CONN_PERIPHERALS_SERVICE_AIDL");
        aidlIntent.setPackage("com.android.settings");
        return aidlIntent;
    }
    public boolean getFingerprintSensorState(){
        boolean ret = false;
        int usbRole = -1;

        try {
            if (mPeripheralsInterface != null) {
                ret = mPeripheralsInterface.getFingerPrintSwitch();
                if (!ret){
                    return false;
                }

                usbRole = mPeripheralsInterface.getUSBRole();
                if (usbRole == 2){ // DEVICE mode: PC connection only
                    return false;
                }else if (usbRole == 1){ // HOST mode: Peripherals only
                    return true;
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        // Here, fingerprint sensor should be powered on, and USB role set to AUTO
        // Check if tablet is plugged to the computer
        if (!isDevicePluggedToPc()){
            return true;
        }

        return false;
    }

    private boolean isDevicePluggedToPc(){
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = mainActivity.registerReceiver(null, ifilter);

        // Are we charging / charged?
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        if (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL) {
            // How are we charging?
            int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
            if (chargePlug == BatteryManager.BATTERY_PLUGGED_USB) {
                Log.d(TAG, "USB plugged");
                return true;
            }
            if (chargePlug == BatteryManager.BATTERY_PLUGGED_AC) {
                Log.d(TAG, "Powered by 3.5mm connector");
                return false;
            }
        }

        return false;
    }

    private void showToastMessage(String msg, int length){
        Toast toast = Toast.makeText(mainActivity, msg, length);
        toast.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 180);
        toast.show();
    }

    public MorphoDevice getDeviceHandle(){
        return morphoDevice;
    }

}
