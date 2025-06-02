package com.kit.photocapture.presentation.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.kit.biometricsdk.R;
import com.kit.photocapture.view.BoxOverlayView;

import java.io.File;
import java.io.FileOutputStream;

public class CapturedPhotoPreviewActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_PATH = "photo_path";

    private ImageView capturedImageView;
    private Button btnSave, btnRetake, btnProceed;
    private String photoPath;
    private BoxOverlayView boxOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captured_photo_preview);

        capturedImageView = findViewById(R.id.image_captured);
        btnSave = findViewById(R.id.btn_save);
        btnRetake = findViewById(R.id.btn_retake);
        btnProceed = findViewById(R.id.btn_proceed);
        boxOverlay = findViewById(R.id.box_overlay);

        photoPath = getIntent().getStringExtra(EXTRA_PHOTO_PATH);

        if (photoPath != null) {
            Bitmap originalBitmap = BitmapFactory.decodeFile(photoPath);
            if (originalBitmap != null) {
                Bitmap rotatedBitmap = rotateBitmap(originalBitmap, 0);

                // Delay to ensure layout is measured
                capturedImageView.post(() -> {
                    Rect box = boxOverlay.getBoxRect();
                    setImageViewInsideBox(rotatedBitmap, box);
                });

                boxOverlay.setBoxState(BoxOverlayView.BoxState.GREEN);
            } else {
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "No photo to display", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSave.setOnClickListener(v -> {
            if (photoPath != null) {
                Bitmap originalBitmap = BitmapFactory.decodeFile(photoPath);
                if (originalBitmap != null) {
                    try {
                        String fileName = "full_photo_" + System.currentTimeMillis() + ".jpg";
                        File dir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "SavedPhotos");
                        if (!dir.exists()) dir.mkdirs();

                        File file = new File(dir, fileName);
                        FileOutputStream out = new FileOutputStream(file);

                        // Save the full image without cropping
                        originalBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                        out.flush();
                        out.close();

                        Toast.makeText(this, "Photo saved to: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to save photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "Error: Bitmap is null", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error: Photo path is null", Toast.LENGTH_SHORT).show();
            }
        });



        btnRetake.setOnClickListener((View v) -> {
            Intent intent = new Intent(CapturedPhotoPreviewActivity.this, PhotoCaptureActivity2.class);
            startActivityForResult(intent, 101); // use any request code (e.g., 101)
            finish();


        });
        btnProceed.setOnClickListener(v -> {
            Log.d("CapturedPhotoPreview", "Proceed clicked. Returning photoPath: " + photoPath);

            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_PHOTO_PATH, photoPath);
            setResult(RESULT_OK, resultIntent);
            finish();
        });


    }

    private Bitmap rotateBitmap(Bitmap source, float angle) {
        android.graphics.Matrix matrix = new android.graphics.Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private Bitmap cropBitmapToBox(Bitmap bitmap, Rect box, int viewWidth, int viewHeight) {
        // Scale box from view to bitmap coordinate system
        float scaleX = (float) bitmap.getWidth() / viewWidth;
        float scaleY = (float) bitmap.getHeight() / viewHeight;

        int left = (int) (box.left * scaleX);
        int top = (int) (box.top * scaleY);
        int width = (int) (box.width() * scaleX);
        int height = (int) (box.height() * scaleY);

        // Ensure bounds are safe
        left = Math.max(0, left);
        top = Math.max(0, top);
        width = Math.min(bitmap.getWidth() - left, width);
        height = Math.min(bitmap.getHeight() - top, height);

        return Bitmap.createBitmap(bitmap, left, top, width, height);
    }

    private void setImageViewInsideBox(Bitmap bitmap, Rect box) {
        // Set the size and position of the ImageView to match the box
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(box.width(), box.height());
        params.leftMargin = box.left;
        params.topMargin = box.top;
        capturedImageView.setLayoutParams(params);

        // Set the image
        capturedImageView.setImageBitmap(bitmap);
        capturedImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
    }

}
