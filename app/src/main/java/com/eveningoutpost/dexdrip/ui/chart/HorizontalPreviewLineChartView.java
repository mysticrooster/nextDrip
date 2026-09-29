package com.eveningoutpost.dexdrip.ui.chart;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import lecho.lib.hellocharts.view.PreviewLineChartView;

/**
 * A [PreviewLineChartView] whose touch handling is restricted to the horizontal axis (see
 * {@link HorizontalLineChartView} for why hello-charts needs this).
 */
public class HorizontalPreviewLineChartView extends PreviewLineChartView {

    private float downY;

    public HorizontalPreviewLineChartView(Context context) {
        super(context);
    }

    public HorizontalPreviewLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public HorizontalPreviewLineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        pinVerticalAxis(event);
        return super.onTouchEvent(event);
    }

    private void pinVerticalAxis(MotionEvent event) {
        if (event.getPointerCount() != 1) return;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = event.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                final float dy = event.getY() - downY;
                if (dy != 0f) {
                    event.offsetLocation(0f, -dy);
                }
                break;
            default:
                break;
        }
    }
}
