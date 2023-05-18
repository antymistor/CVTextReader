package com.example.utils;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import androidx.appcompat.widget.AppCompatTextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Created by aizhiqiang on 2023/2/20
 *
 * @author aizhiqiang@bytedance.com
 */
public class TextViewAdvance extends AppCompatTextView {
    private Context mContext;
    private int mHeight = 0;
    private Handler mHandler;
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

    public void constrainScrollBy(int dy) {
        Rect viewport = new Rect();
        getGlobalVisibleRect(viewport);
        int height = viewport.height();
        int scrollY = getScrollY();
        if(mHeight <= 0){
            mHeight = getLineHeight() * getLineCount();
        }
        //下边界
        Boolean hascometoend = false;
        if (mHeight - scrollY - dy < height) {
            dy = mHeight - scrollY - height;
            hascometoend = true;
        }
        //上边界
        if (scrollY + dy < 0) {
            dy = -scrollY;
        }
        scrollBy(0, dy);
        if(mListener != null) {
            mListener.onProcess(1.0f * getScrollY() / getLineCount() / getLineHeight());
            if(hascometoend){
                mListener.onToEnd();
            }
        }
    }
}
