package com.elyria.swipeback;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;
import android.widget.TextView;

import com.elyria.library.SwipeBackLayout;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SWIPE";
    private static final int DURATION = 300;
    private TextView enableTextView;
    private FrameLayout container;
    private static int bgIndex = 0;
    private boolean isAnimating = false;
    private int pageIndex = 0;
    private int[] bgColors;
    private int screenWidth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableTextView = (TextView) findViewById(R.id.text_enable);
        container = (FrameLayout) findViewById(R.id.container);
        screenWidth = getResources().getDisplayMetrics().widthPixels;
    }

    public void startNewActivity(View view) {
        SwipeBackLayout swipeBackLayout = new SwipeBackLayout(this);
        Log.d(TAG, String.format("this is %s", swipeBackLayout));
        final TextView textView = getTextView();
        swipeBackLayout.setupReactRootView(textView);
        swipeBackLayout.setSwipeBackEnable(true);
        container.addView(swipeBackLayout);
        updateEnableView();
        swipeBackLayout.setSwipeCallback(new SwipeBackLayout.SwipeCallback() {
            @Override
            public void onShouldFinish() {
                container.removeViewAt(pageIndex - 1);
                pageIndex--;
                updateEnableView();
            }

            @Override
            public void onScrollPercent(float scrollPercent) {
                Log.d(TAG, String.format("scrollPercent = %s", scrollPercent));
            }

            @Override
            public void onScrollOverThreshold() {
                Log.d(TAG, "onScrollOverThreshold");
            }
        });
    }

    private void updateEnableView() {
        if (container.getChildCount() > 0) {
            SwipeBackLayout swipeBackLayout = (SwipeBackLayout) container.getChildAt(pageIndex - 1);
            enableTextView.setText(!swipeBackLayout.getSwipeBackEnable() ? R.string.enable : R.string.disable);
        }
        enableTextView.setVisibility(container.getChildCount() > 0 ? View.VISIBLE : View.GONE);
    }

    @NonNull
    private TextView getTextView() {
        final TextView textView = new TextView(this);
        textView.setBackgroundColor(getColors()[bgIndex]);
        bgIndex++;
        if (bgIndex >= getColors().length) {
            bgIndex = 0;
        }
        ValueAnimator enterAnimation = ValueAnimator.ofFloat(screenWidth, 0);
        enterAnimation.setDuration(DURATION);
        enterAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                textView.setTranslationX((Float) valueAnimator.getAnimatedValue());
            }
        });
        textView.setGravity(Gravity.CENTER);
        enterAnimation.start();
        pageIndex++;
        textView.setText(String.format("the page index is %s ", pageIndex));
        textView.setTextColor(Color.BLACK);
        LayoutParams layoutParams = new LayoutParams(MATCH_PARENT, MATCH_PARENT);
        textView.setLayoutParams(layoutParams);
        return textView;
    }

    private int[] getColors() {
        if (bgColors == null) {
            Resources resource = getResources();
            bgColors = new int[]{
                    resource.getColor(R.color.androidColorA),
                    resource.getColor(R.color.androidColorB),
                    resource.getColor(R.color.androidColorC),
                    resource.getColor(R.color.androidColorD),
                    resource.getColor(R.color.androidColorE),
            };
        }
        return bgColors;
    }

    public void enableSwipeBack(View view) {
        if (container.getChildCount() > 0) {
            SwipeBackLayout swipeBackLayout = (SwipeBackLayout) container.getChildAt(pageIndex - 1);
            swipeBackLayout.setSwipeBackEnable(!swipeBackLayout.getSwipeBackEnable());
        }
        updateEnableView();
    }


    @Override
    public void onBackPressed() {
        if (container.getChildCount() > 0) {
            if (!isAnimating) {
                ValueAnimator exitAnimation = ValueAnimator.ofFloat(0, screenWidth);
                exitAnimation.setDuration(DURATION);
                exitAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        isAnimating = true;
                        Float animatedValue = (Float) valueAnimator.getAnimatedValue();
                        container.getChildAt(pageIndex - 1).setTranslationX(animatedValue);
                    }
                });
                exitAnimation.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimating = false;
                        container.removeViewAt(pageIndex - 1);
                        pageIndex--;
                        updateEnableView();
                    }
                });
                exitAnimation.start();
            }
        } else {
            super.onBackPressed();
        }
    }
}
