package com.example.utils;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Environment;

import java.util.ArrayList;


/**
 * Created by aizhiqiang on 2023/2/20
 *
 * @author aizhiqiang@bytedance.com
 */
public class TextViewCreater {
    public static class TextViewPara{
        public int fontsize        =  15;
        public Context basecontext;
        public String txtpath      =  Environment.getExternalStorageDirectory().getPath() +"/test.txt";
        public TextViewAdvance.IProcessListener listener = null;
    }
    public static TextViewAdvance createTextView(TextViewPara para){
        TextViewAdvance view = new TextViewAdvance(para.basecontext);
        view.setTextSize(para.fontsize);
        view.setListener(para.listener);
        view.pushTxt(para.txtpath);
        return view;
    }
}
