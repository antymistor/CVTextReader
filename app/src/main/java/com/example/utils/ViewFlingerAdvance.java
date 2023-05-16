package com.example.utils;

import android.content.Context;
import android.os.Handler;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.widget.OverScroller;

import androidx.core.view.MotionEventCompat;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aizhiqiang on 2023/5/15
 *
 * @author aizhiqiang@bytedance.com
 */
public class ViewFlingerAdvance extends View implements Runnable {
    private static final int INVALID_POINTER = -1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;
    public enum AutoMovingMode{
        None,
        Down,
        UP,
        ForceNone
    }
    public interface IScrollListener {
        void scrollByHeight(int dy);
    }

    ArrayList<IScrollListener> listeners;
    private int mLastFlingY = 0;
    private OverScroller mScroller;
    private VelocityTracker mVelocityTracker;
    private boolean mEatRunOnAnimationRequest = false;
    private boolean mReSchedulePostAnimationCallback = false;
    private AutoMovingMode movingmode = AutoMovingMode.None;
    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private int mTouchSlop;
    private int mLastTouchY;
    private long lastdowntime = 0;
    private boolean isAutoMoving = false;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;

    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public ViewFlingerAdvance(Context context) {
        super(context);
        mScroller = new OverScroller(getContext(), sQuinticInterpolator);
        init(context);
    }

    private void init(Context context) {
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        listeners = new ArrayList<>();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(isAutoMoving || movingmode != AutoMovingMode.None ){
                    doScroll(movingmode == AutoMovingMode.Down ? 160 :
                            movingmode == AutoMovingMode.UP ? -160 : 1);
                }
            }
        }, 1000, 40);
    }

    public void addScrollListener(IScrollListener listener){
        listeners.add(listener);
    }

    public void enableEyeAutoMoving(AutoMovingMode mode){
        if(mode == AutoMovingMode.ForceNone){
            movingmode = AutoMovingMode.None;
            isAutoMoving = false;
        }else{
            movingmode = mode;
        }
    }

    private void doScroll(int dy){
        for(IScrollListener listener : listeners){
            listener.scrollByHeight(dy);
        }
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
           stop();
        }
    }

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        boolean eventAddedToVelocityTracker = false;
        final int action = MotionEventCompat.getActionMasked(event);
        final int actionIndex = MotionEventCompat.getActionIndex(event);
        final MotionEvent vtev = MotionEvent.obtain(event);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                if( (System.currentTimeMillis() - lastdowntime) < 300){
                    isAutoMoving = true;
                }else {
                    isAutoMoving = false;
                    setScrollState(SCROLL_STATE_IDLE);
                    mScrollPointerId = event.getPointerId(0);
                    mLastTouchY = (int) (event.getY() + 0.5f);
                }
                lastdowntime = System.currentTimeMillis();
                break;
            }
            case MotionEventCompat.ACTION_POINTER_DOWN: {
                mScrollPointerId = event.getPointerId(actionIndex);
                mLastTouchY = (int) (event.getY(actionIndex) + 0.5f);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final int index = event.findPointerIndex(mScrollPointerId);

                final int y = (int) (event.getY(index) + 0.5f);
                int dy = mLastTouchY - y;

                if (mScrollState != SCROLL_STATE_DRAGGING) {
                    boolean startScroll = false;

                    if (Math.abs(dy) > mTouchSlop) {
                        if (dy > 0) {
                            dy -= mTouchSlop;
                        } else {
                            dy += mTouchSlop;
                        }
                        startScroll = true;
                    }
                    if (startScroll) {
                        setScrollState(SCROLL_STATE_DRAGGING);
                    }
                }

                if (mScrollState == SCROLL_STATE_DRAGGING) {
                    mLastTouchY = y;
                    doScroll(dy);
                }

                break;
            }
            case MotionEventCompat.ACTION_POINTER_UP: {
                if (event.getPointerId(actionIndex) == mScrollPointerId) {
                    // Pick a new pointer to pick up the slack.
                    final int newIndex = actionIndex == 0 ? 1 : 0;
                    mScrollPointerId = event.getPointerId(newIndex);
                    mLastTouchY = (int) (event.getY(newIndex) + 0.5f);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {
                mVelocityTracker.addMovement(vtev);
                eventAddedToVelocityTracker = true;
                mVelocityTracker.computeCurrentVelocity(1000, mMaxFlingVelocity);
                float yVelocity = -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId);
                if (Math.abs(yVelocity) < mMinFlingVelocity) {
                    yVelocity = 0F;
                } else {
                    yVelocity = Math.max(-mMaxFlingVelocity, Math.min(yVelocity, mMaxFlingVelocity));
                }
                if (yVelocity != 0) {
                    fling((int) yVelocity);
                } else {
                    setScrollState(SCROLL_STATE_IDLE);
                }
                resetTouch();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                resetTouch();
                break;
            }
        }
        if (!eventAddedToVelocityTracker) {
            mVelocityTracker.addMovement(vtev);
        }
        vtev.recycle();
        return true;

    }


    @Override
    public void run() {
        disableRunOnAnimationRequests();
        final OverScroller scroller = mScroller;
        if (scroller.computeScrollOffset()) {
            final int y = scroller.getCurrY();
            int dy = y - mLastFlingY;
            mLastFlingY = y;
            doScroll(dy);
            postOnAnimation();
        }
        enableRunOnAnimationRequests();
    }

    public void fling(int velocityY) {
        mLastFlingY = 0;
        setScrollState(SCROLL_STATE_SETTLING);
        mScroller.fling(0, 0, 0, velocityY, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
        postOnAnimation();
    }

    public void stop() {
        removeCallbacks(this);
        mScroller.abortAnimation();
    }

    private void disableRunOnAnimationRequests() {
        mReSchedulePostAnimationCallback = false;
        mEatRunOnAnimationRequest = true;
    }

    private void enableRunOnAnimationRequests() {
        mEatRunOnAnimationRequest = false;
        if (mReSchedulePostAnimationCallback) {
            postOnAnimation();
        }
    }

    void postOnAnimation() {
        if (mEatRunOnAnimationRequest) {
            mReSchedulePostAnimationCallback = true;
        } else {
            removeCallbacks(this);
            ViewCompat.postOnAnimation(ViewFlingerAdvance.this, this);
        }
    }
}
