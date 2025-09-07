package com.kit.common;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.kit.biometricsdk.R;

public class CustomToastHandler {

    public static void showErrorToast(Context context, String msg) {
        View rootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast, rootView.findViewById(R.id.toast_layout));

        final Toast toast = new Toast(context.getApplicationContext());
        TextView errorText = layout.findViewById(R.id.alertText);
        errorText.setTextColor(ContextCompat.getColor(context, R.color.red500));
        errorText.setText(msg);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    public static void showSuccessToast(Context context, String msg) {
        View rootView = ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast, rootView.findViewById(R.id.toast_layout));

        final Toast toast = new Toast(context.getApplicationContext());
        TextView errorText = layout.findViewById(R.id.alertText);
        errorText.setTextColor(ContextCompat.getColor(context, R.color.green500));
        errorText.setText(msg);
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
