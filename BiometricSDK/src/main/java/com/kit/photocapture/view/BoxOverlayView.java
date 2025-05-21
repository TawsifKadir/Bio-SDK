package com.kit.photocapture.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoxOverlayView extends View {
    private Paint borderPaint;
    private Paint outsidePaint;
    private Rect boxRect;
    private boolean isCompliant = false;

    // Aspect ratio for vertical rectangle (width:height)
    private static final float BOX_ASPECT_RATIO = 2f / 3f; // Adjust as needed
    private static final float BOX_WIDTH_PERCENT = 0.7f; // Box width as % of screen width

    public BoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaints();
    }

    private void initPaints() {
        borderPaint = new Paint();
        borderPaint.setColor(Color.RED);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);
        borderPaint.setAntiAlias(true);

        outsidePaint = new Paint();
        outsidePaint.setColor(Color.BLACK);
        outsidePaint.setAlpha(100); // Semi-transparent dim
    }

    public void setCompliant(boolean compliant) {
        if (isCompliant != compliant) {
            isCompliant = compliant;
            borderPaint.setColor(compliant ? Color.GREEN : Color.RED);
            postInvalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateBoxRect(w, h);
    }

    private void calculateBoxRect(int viewWidth, int viewHeight) {
        // Calculate box width based on screen width percentage
        int boxWidth = (int) (viewWidth * BOX_WIDTH_PERCENT);

        // Calculate height maintaining aspect ratio
        int boxHeight = (int) (boxWidth / BOX_ASPECT_RATIO);

        // Center the box vertically with some top margin
        int topMargin = (int) (viewHeight * 0.1f); // 10% from top
        int left = (viewWidth - boxWidth) / 2;
        int top = topMargin;
        int right = left + boxWidth;
        int bottom = top + boxHeight;

        boxRect = new Rect(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (boxRect == null) {
            calculateBoxRect(getWidth(), getHeight());
        }

        // Draw outer dim
        canvas.drawRect(0, 0, getWidth(), boxRect.top, outsidePaint); // Top
        canvas.drawRect(0, boxRect.top, boxRect.left, boxRect.bottom, outsidePaint); // Left
        canvas.drawRect(boxRect.right, boxRect.top, getWidth(), boxRect.bottom, outsidePaint); // Right
        canvas.drawRect(0, boxRect.bottom, getWidth(), getHeight(), outsidePaint); // Bottom

        // Draw the box with rounded corners
        float cornerRadius = 16f; // Adjust for desired roundness
        canvas.drawRoundRect(
                boxRect.left, boxRect.top, boxRect.right, boxRect.bottom,
                cornerRadius, cornerRadius, borderPaint
        );
    }

    public Rect getBoxRect() {
        if (boxRect == null) {
            calculateBoxRect(getWidth(), getHeight());
        }
        return boxRect;
    }
}

