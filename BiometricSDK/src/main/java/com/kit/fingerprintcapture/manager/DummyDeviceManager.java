package com.kit.fingerprintcapture.manager;

import android.app.Activity;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.callback.DeviceDataCallback;
import com.kit.fingerprintcapture.handlers.FingerprintCaptureHandler;
import com.kit.fingerprintcapture.utils.ImageProc;
import com.morpho.morphosmart.sdk.TemplateList;

import java.io.BufferedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;


public class DummyDeviceManager implements IDeviceManager{

    private  String TAG = "DummyDeviceManager";
    private DeviceDataCallback deviceDataConsumer;
    private Activity mainActivity;


    public DummyDeviceManager(DeviceDataCallback deviceDataConsumer, Activity mainActivity) {
        this.deviceDataConsumer = deviceDataConsumer;
        this.mainActivity = mainActivity;

    }

    @Override
    public long initDevice() {

        try{
            Thread.sleep(1000);
        }catch(Exception exc){

        }
        return 0;
    }

    @Override
    public long openDevice() {
        try{
            Thread.sleep(1000);
        }catch(Exception exc){

        }

        return 0;
    }


    @Override
    public long startCapture() {
        byte[] wsqData = null;
        int width=248;
        int height=448;

        try{
            Thread.sleep(5000);
        }catch(Exception exc){

        }
        InputStream is = null;
        DataInputStream dis = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try{
            is = mainActivity.getResources().openRawResource(R.raw.fp);
            dis = new DataInputStream(is);

            int ret = 0;
            byte[] readBuf = new byte[4096];
            while(true){
                ret = dis.read(readBuf);
                if(ret==-1) break;
                if(ret == 0) continue;
                baos.write(readBuf);
                baos.flush();

            }
            wsqData = baos.toByteArray();
        }catch(Throwable exc){
            Log.e(TAG,"Error while reading wsqData");
            exc.printStackTrace();
            deviceDataConsumer.onFingerprintData(null,0,0,0,-1);
            return -1;
        }finally {
            try{if(is!=null) is.close();}catch(Exception exc){}
            try{if(dis!=null) dis.close();}catch(Exception exc){}
            try{if(baos!=null) baos.close();}catch(Exception exc){}
        }

        if(wsqData==null){
            deviceDataConsumer.onFingerprintData(null,0,0,0,-1);
            return -1;
        }

        byte[] greyData = null;

        try {
            greyData = ImageProc.fromWSQ(wsqData, width, height);
        }catch(Exception exc){
            Log.e(TAG,"Error while decoding wsqData");
            exc.printStackTrace();
        }

        if(greyData==null){
            deviceDataConsumer.onFingerprintData(null,0,0,0,-1);
            return -1;
        }

        deviceDataConsumer.onFingerprintData(greyData,width,height,60,0);

        return 0;
    }

    @Override
    public long closeDevice() {
        return 0;
    }

    @Override
    public long deInitDevice() {
        return 0;
    }

    @Override
    public boolean isDeviceOpen() {
        return true;
    }

    @Override
    public boolean isPermissionAcquired() {
        return true;
    }

}
