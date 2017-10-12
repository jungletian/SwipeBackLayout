package com.elyria.library;

import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import static android.support.v4.widget.ViewDragHelper.DIRECTION_VERTICAL;
import static android.support.v4.widget.ViewDragHelper.EDGE_LEFT;
import static android.support.v4.widget.ViewDragHelper.STATE_DRAGGING;

public class SwipeBackLayout extends FrameLayout {

    private static final String TAG = "SwipeBackLayout";
    // threshold
    private static final float DEFAULT_SCROLL_THRESHOLD = 0.3f;
    private float mScrollThreshold = DEFAULT_SCROLL_THRESHOLD;
    private float mScrollPercent;
    // scrim color
    private static final int DEFAULT_SCRIM_COLOR = 0x99000000;
    private int mScrimColor = DEFAULT_SCRIM_COLOR;
    private double mScrimOpacity;
    // content view
    private View mReactRootView;
    private int mReactRootViewLeft;
    private int mReactRootViewTop;
    private int mScreenWidth;
    // flag
    private boolean mEnable;
    private boolean mInLayout;
    // callback & helper
    private ViewDragHelper mViewDragHelper;
    private SwipeCallback mSwipeCallback;

    public SwipeBackLayout(@NonNull Context context) {
        this(context, null);
    }

    public SwipeBackLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SwipeBackLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
        initView();
    }

    private void initView() {
        mViewDragHelper = ViewDragHelper.create(this, 1f, new ViewDragHelper.Callback() {
            // touch over the threshold of screen (0-0.3f area)
            private boolean mIsScrollOverValid;

            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                boolean edgeTouched = mViewDragHelper.isEdgeTouched(EDGE_LEFT, pointerId);
                mIsScrollOverValid = edgeTouched;
                return edgeTouched && !mViewDragHelper.checkTouchSlop(DIRECTION_VERTICAL, pointerId);
            }

            @Override
            public void onEdgeTouched(int edgeFlags, int pointerId) {
                super.onEdgeTouched(edgeFlags, pointerId);
                mViewDragHelper.captureChildView(mReactRootView, pointerId);
            }

            @Override
            public int getViewHorizontalDragRange(View child) {
                return mScreenWidth;
            }

            @Override
            public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
                super.onViewPositionChanged(changedView, left, top, dx, dy);
                mScrollPercent = Math.abs((float) left / (mReactRootView.getWidth()));
                mReactRootViewLeft = left;
                mReactRootViewTop = top;
                invalidate();
                if (mScrollPercent < mScrollThreshold && !mIsScrollOverValid) {
                    mIsScrollOverValid = true;
                }
                if (mSwipeCallback != null) {
                    mSwipeCallback.onScrollPercent(mScrollPercent);
                    if (mViewDragHelper.getViewDragState() == STATE_DRAGGING
                            && mScrollPercent >= mScrollThreshold && mIsScrollOverValid) {
                        mIsScrollOverValid = false;
                        mSwipeCallback.onScrollOverThreshold();
                    }
                }

                if (mScrollPercent >= 1) {
                    if (mSwipeCallback != null) {
                        mSwipeCallback.onShouldFinish();
                    }
                }
            }

            @Override
            public void onViewReleased(View releasedChild, float xvel, float yvel) {
                final int childWidth = releasedChild.getWidth();
                int left = xvel > 0 || xvel == 0 && mScrollPercent > mScrollThreshold ? childWidth : 0;
                mViewDragHelper.settleCapturedViewAt(left, 0);
                invalidate();
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                return Math.min(child.getWidth(), Math.max(left, 0));
            }
        });
        mViewDragHelper.setEdgeTrackingEnabled(EDGE_LEFT);
    }

    /**
     * Set content view
     */
    public void setupReactRootView(View view) {
        this.addView(view);
        this.mReactRootView = view;
    }

    /**
     * Set callback
     */
    public void setSwipeCallback(SwipeCallback swipeCallback) {
        this.mSwipeCallback = swipeCallback;
    }

    /**
     * Set enable or disable SwipeBack
     */
    public void setSwipeBackEnable(boolean enable) {
        this.mEnable = enable;
    }

    /**
     * Get SwipeBack's mEnable
     */
    public boolean getSwipeBackEnable() {
        return this.mEnable;
    }


    /**
     * Set scrim color
     *
     * @param color to use in 0xAARRGGBB format.
     */
    public void setScrimColor(int color) {
        mScrimColor = color;
        invalidate();
    }

    /**
     * Set scroll threshold, close the activity or remove view when scrollPercent over this value
     *
     * @param threshold range of 0 ~ 1f
     */
    public void setScrollThreshold(float threshold) {
        if (threshold >= 1.0f || threshold <= 0) {
            throw new IllegalArgumentException("Threshold value should be between 0 and 1.0");
        }
        mScrollThreshold = threshold;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        Log.d(TAG, String.format("dispatchTouchEvent() this is %s, enable = %s", this, mEnable));
        return !mEnable || super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d(TAG, String.format("onInterceptTouchEvent() this is %s, enable = %s", this, mEnable));
        return mEnable && mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, String.format("onTouchEvent() this is %s, enable = %s", this, mEnable));
        if (!mEnable) return false;
        mViewDragHelper.processTouchEvent(event);
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        Log.d(TAG, String.format("onLayout() this is %s, enable = %s", this, mEnable));
        mInLayout = true;
        if (mReactRootView != null) {
            mReactRootView.layout(mReactRootViewLeft, mReactRootViewTop,
                    mReactRootViewLeft + mReactRootView.getMeasuredWidth(),
                    mReactRootViewTop + mReactRootView.getMeasuredHeight());
            mInLayout = false;
        }
    }

    @Override
    public void requestLayout() {
        if (!mInLayout) {
            super.requestLayout();
        }
    }

    @Override
    public void computeScroll() {
        mScrimOpacity = 1 - mScrollPercent;
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        Log.d(TAG, String.format("drawChild() this is %s, enable = %s", this, mEnable));
        final boolean drawContent = child == mReactRootView;
        boolean ret = super.drawChild(canvas, child, drawingTime);
        if (mScrimOpacity > 0 && drawContent
                && mViewDragHelper.getViewDragState() != ViewDragHelper.STATE_IDLE) {
            drawScrim(canvas, child);
        }
        return ret;
    }

    private void drawScrim(Canvas canvas, View child) {
        final int baseAlpha = (mScrimColor & 0xff000000) >>> 24;
        final int alpha = (int) (baseAlpha * mScrimOpacity);
        final int color = alpha << 24 | (mScrimColor & 0xffffff);
        canvas.clipRect(0, 0, child.getLeft(), getHeight());
        canvas.drawColor(color);
    }

    public interface SwipeCallback {
        /**
         * Finish activity or remove view
         */
        void onShouldFinish();

        /**
         * The scroll percent of screen
         *
         * @param scrollPercent range of 0 ~ 1f
         */
        void onScrollPercent(float scrollPercent);

        /**
         * Called when scroll over threshold
         *
         * @see #mScrollThreshold
         */
        void onScrollOverThreshold();
    }
}
