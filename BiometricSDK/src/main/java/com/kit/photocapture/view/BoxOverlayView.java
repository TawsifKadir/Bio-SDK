package com.kit.photocapture.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

public class BoxOverlayView extends View {

    public enum BoxState {
        RED, YELLOW, GREEN
    }
    private Paint borderPaint;
    private Paint outsidePaint;
    private Rect boxRect;
    private boolean isCompliant = false;

    private BoxState boxState = BoxState.RED;

    public void setBoxState(BoxState state) {
        if (this.boxState != state) {
            this.boxState = state;
            switch (state) {
                case RED:
                    borderPaint.setColor(Color.RED);
                    break;
                case YELLOW:
                    borderPaint.setColor(Color.YELLOW);
                    break;
                case GREEN:
                    borderPaint.setColor(Color.GREEN);
                    break;
            }
            postInvalidate();
        }
    }


    // Aspect ratio for vertical rectangle (width:height)
    private static final float BOX_ASPECT_RATIO = 2f / 3f; // Adjust as needed
    private static final float BOX_WIDTH_PERCENT = 0.7f; // Box width as % of screen width
    private static final float MIN_TOP_MARGIN_DP = 150f; // Minimum top margin to account for status text (matches your TextView margin)
    private static final float BOTTOM_MARGIN_DP = 130f; // Space from bottom (e.g., for camera button or spacing)


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

        // Convert minimum top margin from dp to pixels
        float density = getResources().getDisplayMetrics().density;
        int minTopMarginPx = (int) (MIN_TOP_MARGIN_DP * density);

        // Calculate available space for the box (accounting for status text at top and capture button at bottom)
        int bottomMarginPx = (int)(BOTTOM_MARGIN_DP * density);
        int availableHeight = viewHeight - minTopMarginPx - bottomMarginPx;


        // If the calculated box height is too large for the available space, scale it down
        if (boxHeight > availableHeight) {
            float scaleFactor = (float) availableHeight / boxHeight;
            boxWidth = (int)(boxWidth * scaleFactor);
            boxHeight = availableHeight;
        }

        // Center the box horizontally and position it below the status text
        int left = (viewWidth - boxWidth) / 2;
        int top = minTopMarginPx;
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