package com.kit.photocapture.view;

import android.annotation.SuppressLint;
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

    public BoxOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        borderPaint = new Paint();
        borderPaint.setColor(Color.RED);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5f);

        outsidePaint = new Paint();
        outsidePaint.setColor(Color.BLACK);
        outsidePaint.setAlpha(100); // transparent dim
    }

    public void setCompliant(boolean compliant) {
        isCompliant = compliant;
        borderPaint.setColor(compliant ? Color.GREEN : Color.RED);
        invalidate();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int boxWidth = width / 2;
        int boxHeight = height / 3;

        int left = (width - boxWidth) / 2;
        int top = (height - boxHeight) / 2;
        int right = left + boxWidth;
        int bottom = top + boxHeight;

        boxRect = new Rect(left, top, right, bottom);

        // Draw outer dim
        canvas.drawRect(0, 0, width, top, outsidePaint);
        canvas.drawRect(0, top, left, bottom, outsidePaint);
        canvas.drawRect(right, top, width, bottom, outsidePaint);
        canvas.drawRect(0, bottom, width, height, outsidePaint);

        // Draw the box
        canvas.drawRect(boxRect, borderPaint);
    }

    public Rect getBoxRect() {
        return boxRect;
    }
}

