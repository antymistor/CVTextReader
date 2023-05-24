package com.example.utils;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import androidx.appcompat.widget.AppCompatImageView;

/**
 * Created by aizhiqiang on 2023/5/15
 *
 * @author aizhiqiang@bytedance.com
 */
public class ImageViewAdvance extends AppCompatImageView {
    private Activity mContext;
    private Bitmap mOriBitmap;
    private int mOriWidth = 0;
    private int mOriHeight = 0;
    private int mNowCursor = 0;
    private int mDisplayHeight = 0;
    private int mDisplayWidth = 0;
    private Boolean mNeedUpdate = false;
    private final Matrix mMatrix;
    private int resid = -1;
    public ImageViewAdvance(Activity context) {
        super(context);
        mContext = context;
        mMatrix = new Matrix();
    }
    public void setOriBitmap(Bitmap bitmap){
        resid = -1;
        mOriBitmap = bitmap;
        mNeedUpdate = true;
    }

    public void setOriBitmapResid(int id){
        resid = id;
        mOriBitmap = null;
        mNeedUpdate = true;
    }

    class LoadBitmapThread extends Thread{//继承Thread类
        @Override
        public void run(){
            if( (mOriBitmap == null && resid == -1) || !mNeedUpdate){
                return;
            }
            if(mOriBitmap == null){
                mOriBitmap = BitmapFactory.decodeResource(getResources(), resid);
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

            mContext.runOnUiThread(()->{
                setScaleType(ScaleType.MATRIX);
                mMatrix.postScale(1.0f, 1.0f);
                setImageMatrix(mMatrix);
                setImageBitmap(mOriBitmap);
            });
        }
    }

    public void loadbitmap(){
        new LoadBitmapThread().start();
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
