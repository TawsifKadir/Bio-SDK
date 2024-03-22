package com.kit.common;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.kit.biometricsdk.R;

public class CustomToastHandler {

    public static void showErrorToast(Context context, String msg){
        View rootView = ((Activity)context).getWindow().getDecorView().findViewById(android.R.id.content);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.custom_toast,(ViewGroup) rootView.findViewById(R.id.toast_layout));

        final Toast toast = new Toast(context.getApplicationContext());
        TextView errorText = layout.findViewById(R.id.alertText);
        errorText.setText(msg);
        ImageView image = layout.findViewById(R.id.imageLogo);
        image.setImageResource(R.mipmap.ic_launcher);
        toast.setGravity(Gravity.BOTTOM,0,100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}
