package com.kit.fingerprintcapture.utils;


import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

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


    private FrameLayout loadingOverlay;

    public void showLoading() {
        if (loadingOverlay == null) {
            // Create a full-screen overlay
            loadingOverlay = new FrameLayout(this);
            loadingOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
            ));
            loadingOverlay.setBackgroundColor(0x80000000); // semi-transparent black
            loadingOverlay.setClickable(true);
            loadingOverlay.setFocusable(true);

            // Add a ProgressBar at center
            ProgressBar progressBar = new ProgressBar(this);
            FrameLayout.LayoutParams pbParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            pbParams.gravity = Gravity.CENTER;
            loadingOverlay.addView(progressBar, pbParams);
        }

        // Attach overlay to the root view
        ViewGroup root = findViewById(android.R.id.content);
        if (loadingOverlay.getParent() == null) {
            root.addView(loadingOverlay);
        }
    }

    public void hideLoading() {
        if (loadingOverlay != null) {
            ViewGroup root = findViewById(android.R.id.content);
            root.removeView(loadingOverlay);
        }
    }

}
