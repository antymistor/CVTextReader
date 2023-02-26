package com.example.utils;
import android.content.Context;
import android.graphics.Color;
import android.os.Environment;


/**
 * Created by aizhiqiang on 2023/2/20
 *
 * @author aizhiqiang@bytedance.com
 */
public class TextViewCreater {
    public static class TextViewPara{
        public int fontsize        =  15;
        public int textcolor       = Color.parseColor("#000000");
        public int backgroundcolor = Color.parseColor("#ccaa88");
        public Context basecontext;
        public String txtpath      =  Environment.getExternalStorageDirectory().getPath() +"/test.txt";
        public TextViewAdvance.IProcessListener listener = null;
    }
    public static TextViewAdvance createTextView(TextViewPara para){
        TextViewAdvance view = new TextViewAdvance(para.basecontext);
        view.setBackgroundColor(para.backgroundcolor);
        view.setTextColor(para.textcolor);
        view.setTextSize(para.fontsize);
        view.pushTxt(para.txtpath);
        view.setListener(para.listener);
        return view;
    }
}
