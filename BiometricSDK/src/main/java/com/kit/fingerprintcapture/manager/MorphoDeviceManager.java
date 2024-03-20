package com.kit.fingerprintcapture.manager;

import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ContextWrapper;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;


import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.idemia.peripherals.PeripheralsPowerInterface;
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
import com.morpho.morphosmart.sdk.Template;
import com.morpho.morphosmart.sdk.TemplateFVPType;
import com.morpho.morphosmart.sdk.TemplateList;
import com.morpho.morphosmart.sdk.TemplateType;

import java.nio.ByteBuffer;
import java.util.Observable;
import java.util.Observer;

public class MorphoDeviceManager implements IDeviceManager,Observer{

    private  String TAG = "MorphoDeviceManager02";
    private DeviceDataCallback deviceDataConsumer;
    private Activity mainActivity;

    private MorphoDevice morphoDevice;

    private boolean capturing = false;
    private boolean deviceIsSet = false;

    private byte[] mImageData;
    private int mImageWidth;
    private int mImageHeight;
    private int mQualityScore;

    private PeripheralsPowerInterface mPeripheralsInterface;

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


    public MorphoDeviceManager(DeviceDataCallback deviceDataConsumer, Activity mainActivity) {
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

        try {
            if(!mainActivity.bindService(getAidlIntent(), serviceConn, Service.BIND_AUTO_CREATE)) {
                Log.e(TAG, "System couldn't find the service");
                Toast.makeText(mainActivity, "System couldn't find peripherals service", Toast.LENGTH_SHORT).show();
                deviceIsSet = false;
                return ErrorCodes.CLASS_NOT_INSTANTIATED;
            }

            // On Morphotablet, 3rd parameter (enableWakeLock) must always be true
            USBManager.getInstance().initialize(mainActivity, "com.morpho.morphosample.USB_ACTION", true);
            md = new MorphoDevice();
            CustomInteger nbUsbDevice = new CustomInteger();
            ret = md.initUsbDevicesNameEnum(nbUsbDevice);

            if (ret != ErrorCodes.MORPHO_OK)
                return ret;

            if (nbUsbDevice.getValueOf() != 1) {
                mainActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        android.app.AlertDialog alertDialog = new android.app.AlertDialog.Builder(mainActivity).create();
                        alertDialog.setCancelable(false);
                        alertDialog.setTitle(R.string.app_name);
                        alertDialog.setMessage(mainActivity.getString(R.string.noAccessToDevice));
                        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mainActivity.finish();
                            }
                        });
                        alertDialog.show();
                    }
                });

                return nbUsbDevice.getValueOf();

            } else {
                String sensorName = md.getUsbDeviceName(0); // We use first CBM found
                ret = md.openUsbDevice(sensorName, 0);

                if (ret != ErrorCodes.MORPHO_OK) {
                    mainActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showToastMessage("Error opening USB device", Toast.LENGTH_SHORT);
                            mainActivity.finish();
                        }
                    });


                    return ret;
                }
            }
        }catch(Exception exc){
            Log.e(TAG, "initMorphoDevice Error");
            return ErrorCodes.CLASS_NOT_INSTANTIATED;
        }
        morphoDevice=md;
        deviceIsSet = true;
        return ErrorCodes.MORPHO_OK;
    }

    @Override
    public long openDevice() {
        if(!deviceIsSet){
            long ret = initDevice();
            if(ret!=ErrorCodes.MORPHO_OK) return ret;
        }
        return ErrorCodes.MORPHO_OK;
    }

    @Override
    public long startCapture() {
        long ret = ErrorCodes.MORPHO_OK;
        if (!capturing){
            capturing = true;
            ret = morphoDeviceCapture();
        }
        else if (capturing && deviceIsSet) {
            ret = closeDevice();
            capturing = false;
            deviceIsSet = false;
        }
        else{
            showToastMessage("Device is being initialized, please try again", Toast.LENGTH_SHORT);
            ret = ErrorCodes.MORPHOERR_OTP_NOT_INITIALIZED;
        }

        return 0;
    }

    @Override
    public long closeDevice() {
        long ret = ErrorCodes.MORPHO_OK;
        try {
            mainActivity.unbindService(serviceConn);
            ret = closeMorphoDevice();
        }catch(Exception exc){
            Log.e(TAG,"In CloseDevice error : "+exc.getMessage());
        }
        deviceIsSet = false;
        return ret;
    }

    @Override
    public long deInitDevice() {
        long ret = ErrorCodes.MORPHO_OK;
        if(deviceIsSet){
            ret = closeMorphoDevice();
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
                            MorphoDeviceManager.this);
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
                    MorphoDeviceManager.this.capturing = false;
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

}
