package com.example.utils;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import com.h6ah4i.android.widget.verticalseekbar.VerticalSeekBar;

/**
 * Created by aizhiqiang on 2023/3/5
 *
 * @author aizhiqiang@bytedance.com
 */
public class SeekbarAdvance extends VerticalSeekBar {


    public SeekbarAdvance(Context context) {
        super(context);
    }

    public SeekbarAdvance(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SeekbarAdvance(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public interface OnClickThumbListener{
        void onClickThumb();
    }
    private OnClickThumbListener ClickThumbListener;
    private long lasttouchdown = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if (isTouchInThumb(event, getThumb().getBounds())){
                    lasttouchdown = System.currentTimeMillis();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isTouchInThumb(event, getThumb().getBounds()) &&
                        (System.currentTimeMillis() - lasttouchdown) < 150){
                    Log.e("aizhiqiang", "click on processbar");
                    if (ClickThumbListener != null)
                        ClickThumbListener.onClickThumb();
                }
                break;
        }
        super.onTouchEvent(event);
        return true;
    }

    /**
     * 判断MotionEvent事件是否位于thumb上
     * @param event
     * @param thumbBounds
     * @return
     */
    private boolean isTouchInThumb(MotionEvent event, Rect thumbBounds){
        float x = event.getX();
        float y = event.getY();
        final int marigin = 10;
        if (x >= thumbBounds.left - marigin && x <= thumbBounds.right + marigin
                && y >= thumbBounds.top - marigin * 2 && y <= thumbBounds.bottom + marigin * 2)
            return true;
        return false;
    }
    public void setOnClickThumbListener(OnClickThumbListener listener){
        this.ClickThumbListener = listener;
    }


}
