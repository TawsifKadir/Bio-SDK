package com.kit.fingerprintcapture.utils;


import android.app.Dialog;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.kit.biometricsdk.R;

public class BaseActivityArr extends AppCompatActivity {
    private long lastClickTime = 0;
    int j = 0;
    private static final long CLICK_DEBOUNCE_INTERVAL = 600;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {

        Log.d("anik", "j : "+ j++);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < CLICK_DEBOUNCE_INTERVAL) {
                return true;
            }
            lastClickTime = currentTime;
        }
        return super.dispatchTouchEvent(ev);
    }


    private FrameLayout loadingOverlay;
    private Dialog progressDialog2;

    public void showModifiableLoading(String message) {
        if (progressDialog2 == null) {
            progressDialog2 = createProgressDialog(this, message, false);
        } else {
            if (progressDialog2.isShowing()) {
                progressDialog2.hide();
            }
            progressDialog2 = createProgressDialog(this, message, false);
        }
        progressDialog2.show();
    }



    public void hideModifiableLoading() {
        if (progressDialog2 != null && progressDialog2.isShowing()){
            progressDialog2.hide();
        }
    }


    public static Dialog createProgressDialog(Context context, boolean isCancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(isCancelable);
        builder.setView(R.layout.progress_dialog2);
        return builder.create();
    }

    public static Dialog createProgressDialog(Context context, String message, boolean isCancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setCancelable(isCancelable);
        View view = LayoutInflater.from(context).inflate(R.layout.progress_dialog2, null);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        tvTitle.setText(message);
        builder.setView(view);
        return builder.create();
    }

}
