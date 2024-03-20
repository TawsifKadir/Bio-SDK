package com.kit.fingerprintcapture.adapters;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.kit.BuildConfig;
import com.kit.biometricsdk.R;
import com.kit.fingerprintcapture.model.NoFingerprintReason;

import java.util.List;


public class FingerprintExceptionListAdapter extends BaseAdapter {
    Context context;

    List<NoFingerprintReason> reasons;
    LayoutInflater inflter;

    private String TAG = "FingerprintExceptionListAdapter";
    public FingerprintExceptionListAdapter(Context applicationContext,  List<NoFingerprintReason> reasons) {
        this.context = applicationContext;
        this.reasons = reasons;
        inflter = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        if(reasons!=null) return reasons.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if(false) {
            view = inflter.inflate(R.layout.no_fingerprint_reason_items, null);
            TextView names = (TextView) view.findViewById(R.id.textView);
            if (reasons != null && reasons.get(position) != null)
                names.setText(reasons.get(position).getNoFingerprintReasonText());
        }else{

            view = LayoutInflater.from(context).inflate(R.layout.no_fingerprint_reason_items, parent,false);
            if(view.getBackground()!=null) {
                if (BuildConfig.isDebug) {
                    Log.d(TAG, "TAWSIF & FAISAL DEBUG -> Background drawable is " + view.getBackground().toString());
                }
            }
            view.setBackgroundColor(Color.WHITE);

            TextView names = (TextView) view.findViewById(R.id.textView);
            if (reasons != null && reasons.get(position) != null)
                names.setText(reasons.get(position).getNoFingerprintReasonText());
        }
        return view;
    }
}
