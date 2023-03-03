package com.example.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import android.view.VelocityTracker;
import android.widget.OverScroller;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.MotionEventCompat;
import androidx.core.view.VelocityTrackerCompat;
import androidx.core.view.ViewCompat;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;


/**
 * Created by aizhiqiang on 2023/2/20
 *
 * @author aizhiqiang@bytedance.com
 */
public class TextViewAdvance extends AppCompatTextView {
    private Context mContext;
    private int mHeight = 0;
    private static final int INVALID_POINTER = -1;
    public static final int SCROLL_STATE_IDLE = 0;
    public static final int SCROLL_STATE_DRAGGING = 1;
    public static final int SCROLL_STATE_SETTLING = 2;

    private Handler mHandler;
    private int mScrollState = SCROLL_STATE_IDLE;
    private int mScrollPointerId = INVALID_POINTER;
    private VelocityTracker mVelocityTracker;
    private int mLastTouchY;
    private int mTouchSlop;
    private int mMinFlingVelocity;
    private int mMaxFlingVelocity;
    private long lastdowntime = 0;
    private boolean isAutoMoving = false;

    public enum AutoMovingMode{
        None,
        Down,
        UP,
        ForseNone
    }
    private AutoMovingMode movingmode = AutoMovingMode.None;
    private final ViewFlinger mViewFlinger = new ViewFlinger();

    //f(x) = (x-1)^5 + 1
    private static final Interpolator sQuinticInterpolator = new Interpolator() {
        @Override
        public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * t * t * t + 1.0f;
        }
    };

    public TextViewAdvance(Context context) {
        this(context, null);
    }

    public TextViewAdvance(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextViewAdvance(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context);
    }

    private void init(Context context) {
        mHandler = new Handler();
        this.mContext = context;
        final ViewConfiguration vc = ViewConfiguration.get(context);
        mTouchSlop = vc.getScaledTouchSlop();
        mMinFlingVelocity = vc.getScaledMinimumFlingVelocity();
        mMaxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(isAutoMoving || movingmode != AutoMovingMode.None ){
                    scrollBy(0, movingmode == AutoMovingMode.Down ? 160 :
                                   movingmode == AutoMovingMode.UP ? -160 : 1);
                    if(mListener!= null && mHeight > 0) {
                        mListener.onProcess(1.0f * getScrollY() / getLineCount() / getLineHeight());
                    }
                }
            }
        }, 1000, 40);
    }

    public void enableEyeAutoMoving(AutoMovingMode mode){
        if(mode == AutoMovingMode.ForseNone){
            movingmode = AutoMovingMode.None;
            isAutoMoving = false;
        }else{
            movingmode = mode;
        }
    }

    public void pushTxt(String path){
        mHandler.post(() -> {
            if(!TextUtils.isEmpty(path)){
                File file = new File(path);
                if(file.exists()){
                    InputStreamReader streamreader = null;
                    try {
                        streamreader = new InputStreamReader(new FileInputStream(file), FileCharsetDetector.GetCharset(file));
                        BufferedReader bufferreader = new BufferedReader(streamreader);
                        StringBuffer sb = new StringBuffer("");
                        String line;


                        ArrayList<Pair<String, Float>> list = new ArrayList<Pair<String, Float>>();
                        int linecount = 0;
                        Pattern p =Pattern.compile("^.{0,10}第.{1,5}[章节回].{0,30}");
                        int fontsperLine = getFontsCntPerLine();

                        while ((line = bufferreader.readLine()) != null) {
                            sb.append(line);
                            sb.append("\n");
                            if(p.matcher(line).find()){
                                list.add(new Pair<>(line, (float)linecount));
                            }
                            linecount += Math.ceil(1.0f* line.length() / fontsperLine) ;
                        }
                        setText(sb.toString());
                        mHeight = getLineHeight() * getLineCount();
                        Log.e("aizhiqing", "aizhiqiang height = " + mHeight);
                        if(mListener!=null){
                            mListener.onGetList(list, linecount);
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        });
    }

    public int getFontsCntPerLine() {
        //update font realsize
        String text = "测试中文";
        TextPaint newPaint = new TextPaint();
        float textSize = getResources().getDisplayMetrics().scaledDensity * fontsize;
        newPaint.setTextSize(textSize);
        float newPaintWidth = newPaint.measureText(text);
        return (int) (4 * getWidth() / newPaintWidth );
    }

    private float fontsize = 15;
    @Override
    public void setTextSize(float size) {
        super.setTextSize(size);
        fontsize = size;
    }

    private int themeindex = 0;
    public static class themeitem{
        int backResId = -1;
        int fontcolor = -1;
        int backgroundcolor = -1;
        public themeitem (int id, int font, int back){
            backResId = id;
            fontcolor = font;
            backgroundcolor = back;
        }
    }
    private ArrayList<themeitem> themelist;
    public void setBacklist(ArrayList<themeitem> list){
        themelist = list;
        setThemeindex(0);
    }
    public void setThemeindex(int index){
        if(themelist != null){
            themeindex = Math.max(0, Math.min(index , themelist.size() - 1));
            if(themelist.get(themeindex).backgroundcolor != -1){
                setBackgroundColor(themelist.get(themeindex).backgroundcolor);
            }
            if(themelist.get(themeindex).fontcolor != -1){
                setTextColor(themelist.get(themeindex).fontcolor);
            }
            if(themelist.get(themeindex).backResId != -1){
                setBackgroundResource(themelist.get(themeindex).backResId);
            }
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
//                    Log.e("aizhiqiang", "enable auto moving timecost = " + (System.currentTimeMillis() - lastdowntime));
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
                    constrainScrollBy(dy);
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
                    mViewFlinger.fling((int) yVelocity);
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

    private void resetTouch() {
        if (mVelocityTracker != null) {
            mVelocityTracker.clear();
        }
    }

    private void setScrollState(int state) {
        if (state == mScrollState) {
            return;
        }
        mScrollState = state;
        if (state != SCROLL_STATE_SETTLING) {
            mViewFlinger.stop();
        }
    }

    public interface IProcessListener {
        void onProcess(float progress);
        void onToEnd();
        void onGetList(ArrayList<Pair<String, Float>> list, long linesum);
    }

    private IProcessListener mListener = null;
    public void setListener(IProcessListener listener_){
        mListener = listener_;
    }
    public void seektopos(float pos){
        mHandler.post(() -> {
            if(mHeight <= 0){
                mHeight = getLineHeight() * getLineCount();
            }
            if(mHeight > 0) {
                scrollTo(0, (int) (mHeight * pos));
            }
        });
    }

    public void seekbypos(int step){
        mHandler.post(() -> {
            scrollBy(0, step);
            mListener.onProcess(1.0f * getScrollY() / getLineCount() / getLineHeight());
        });
    }

    private void constrainScrollBy(int dy) {
        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        int height = viewport.height();
        int scrollY = getScrollY();
        if(mHeight <= 0){
            mHeight = getLineHeight() * getLineCount();
        }
        if(mListener != null) {
            mListener.onProcess(1.0f * scrollY / getLineCount() / getLineHeight());
        }
        //下边界
        if (mHeight - scrollY - dy < height) {
            dy = mHeight - scrollY - height;
            if(mListener != null) {
                mListener.onToEnd();
            }
        }
        //上边界
        if (scrollY + dy < 0) {
            dy = -scrollY;
        }
        scrollBy(0, dy);
    }

    private class ViewFlinger implements Runnable {

        private int mLastFlingY = 0;
        private OverScroller mScroller;
        private boolean mEatRunOnAnimationRequest = false;
        private boolean mReSchedulePostAnimationCallback = false;

        public ViewFlinger() {
            mScroller = new OverScroller(getContext(), sQuinticInterpolator);
        }

        @Override
        public void run() {
            disableRunOnAnimationRequests();
            final OverScroller scroller = mScroller;
            if (scroller.computeScrollOffset()) {
                final int y = scroller.getCurrY();
                int dy = y - mLastFlingY;
                mLastFlingY = y;
                constrainScrollBy(dy);
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
                ViewCompat.postOnAnimation(TextViewAdvance.this, this);
            }
        }
    }
}
