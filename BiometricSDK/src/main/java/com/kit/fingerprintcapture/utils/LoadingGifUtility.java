package com.kit.fingerprintcapture.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

public class LoadingGifUtility {

    private static final String TAG = "LoadingGifUtility";
    private static boolean isLoadingGifRunning = false;

    // Start the loading GIF
    public static void startLoading(Context context, ImageView imageView, int gifResId) {
        if (!isLoadingGifRunning) {
            Log.d(TAG, "Entered the loading loop");
            Glide.with(context)
                    .asGif()
                    .load(gifResId)
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(imageView);
            isLoadingGifRunning = true;
        }
    }

    // Stop the loading GIF and clear the ImageView
    public static void stopLoading(ImageView imageView) {
        if (isLoadingGifRunning) {
            // Clear the ImageView and stop any ongoing animations
            imageView.setImageDrawable(null);
            isLoadingGifRunning = false;
        }
    }

    // Load a normal image after stopping the GIF
    public static void loadImage(Context context, ImageView imageView, int imageResId) {
        // Ensure the GIF is stopped first
        stopLoading(imageView);

        // Load a static image into the ImageView
        Glide.with(context)
                .load(imageResId)
                .into(imageView);
    }

    // Load a Bitmap after stopping the GIF
    public static void loadBitmap(ImageView imageView, Bitmap bitmap) {
        // Ensure the GIF is stopped first
        stopLoading(imageView);

        // Load the Bitmap into the ImageView
        imageView.setImageBitmap(bitmap);
    }

}

