package com.example.utils;
import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Size;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;


/**
 * Created by aizhiqiang on 2023/2/20
 *
 * @author aizhiqiang@bytedance.com
 */
public class TextViewCreater {
    public static class TextViewPara{
        public int fontsize        =  15;
        public int textcolor       = Color.parseColor("#000000");
        public int backgroundcolor = Color.parseColor("#bb9977");
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
