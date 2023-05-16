package com.example.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.view.Display;
import android.widget.ImageView;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by aizhiqiang on 2023/5/15
 *
 * @author aizhiqiang@bytedance.com
 */
public class ImageViewAdvance extends AppCompatImageView {
    private Context mContext;
    private Bitmap mOriBitmap;
    private Bitmap mDestBitmap;
    private int mOriWidth = 0;
    private int mOriHeight = 0;
    private int mNowCursor = 0;
    private int mDisplayHeight = 0;
    private int mDisplayWidth = 0;
    private Boolean mNeedUpdate = false;
    private final Matrix mMatrix;
    public ImageViewAdvance(Context context) {
        super(context);
        mContext = context;
        mMatrix = new Matrix();
    }
    public void setOriBitmap(Bitmap bitmap){
        mOriBitmap = bitmap;
        mNeedUpdate = true;
    }

    public void loadbitmap(){
        if(mOriBitmap == null || !mNeedUpdate){
            return;
        }
        mDisplayWidth = getWidth();
        mDisplayHeight = getHeight();
        if(mDisplayWidth <= 0 || mDisplayHeight <= 0){
            return;
        }
        float scale = 1.0f * mDisplayWidth / mOriBitmap.getWidth();
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        mOriBitmap = Bitmap.createBitmap(mOriBitmap, 0, 0, mOriBitmap.getWidth(), mOriBitmap.getHeight(), matrix, true);
        mOriWidth  = mOriBitmap.getWidth();
        mOriHeight = mOriBitmap.getHeight();
        int heightcnt = (int)Math.ceil(1.0f * mDisplayHeight / mOriHeight) + 1;

        Bitmap result = Bitmap.createBitmap(mOriWidth, mOriHeight * heightcnt, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();
        paint.setDither(true);
        for(int i=0; i != heightcnt; ++i){
            canvas.drawBitmap(mOriBitmap, 0, mOriHeight * i, paint);
        }
        mOriBitmap = result;
        mNeedUpdate = false;

        setScaleType(ScaleType.MATRIX);
        mMatrix.postScale(1.0f, 1.0f);
        setImageMatrix(mMatrix);
        setImageBitmap(mOriBitmap);
    }

    public void draw(){
        mMatrix.setTranslate(0.0f, -1.0f * mNowCursor);
        setImageMatrix(mMatrix);
    }
    public void constrainScrollBy(int dy){
        if(mOriHeight <=0){
            return;
        }
        mNowCursor = (mNowCursor + dy) % mOriHeight;
        if(mNowCursor < 0){
            mNowCursor += mOriHeight;
        }
        draw();
    }
}
