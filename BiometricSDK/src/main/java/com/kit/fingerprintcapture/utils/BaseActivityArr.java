package com.kit.fingerprintcapture.utils;


import android.util.Log;
import android.view.MotionEvent;

import androidx.appcompat.app.AppCompatActivity;

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
}
