package com.example.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;


/**
 * Created by aizhiqiang on 2023/5/25
 *
 * @author aizhiqiang@bytedance.com
 */
public class SearchFileProvider {
    public static class FileBean{
        public String fileName;
        public String filePath;
        FileBean(String na, String fi){
            fileName = na;
            filePath = fi;
        }
    }
    public enum operateType{
        SET,
        READ
    }
    public interface ISearchListener {
        void onSearchFinish();
        void onSearchFile(FileBean f);
    }
    ArrayList<FileBean> mlist;
    Context mContext;
    ISearchListener mListenr;
    Integer maxCnt = -1;
    Integer currentSize = 0;
    Boolean isFinished = false;
    Boolean hasStarted = false;
    private static SearchFileProvider instance;

    public void setLisenter(ISearchListener listenr){
        mListenr = listenr;
    }

    public SearchFileProvider(@Nullable Context context){
        mlist = new ArrayList<FileBean>();
        mContext = context;
    }

    public static synchronized SearchFileProvider getInstance(Context con){
        if(instance == null){
            instance = new SearchFileProvider(con);
        }
        return  instance;
    }

    public void setMaxCnt(Integer maxCnt_){
        maxCnt = maxCnt_;
    }

    public Boolean isFinished(){
        return isFinished;
    }

    public Boolean hasStarted(){
        return hasStarted;
    }

    public synchronized ArrayList<FileBean> getOrWriteList(@Nullable FileBean f, operateType type){
        ArrayList<FileBean> list = null;
        if(type == operateType.SET){
            mlist.add(f);
            if(null !=mListenr){
                mListenr.onSearchFile(f);
            }
        }else{
            list = (ArrayList<FileBean>) mlist.clone();
        }
        return list;
    }

    class searchLocalImageFileThread extends Thread{//继承Thread类
        @Override
        public void run(){
            isFinished = false;
            long time = System.currentTimeMillis();
            Cursor cursor = mContext.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range")
                    byte[] data = cursor.getBlob(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    String absolutePath = new String(data, 0, data.length - 1);
                    if( absolutePath.isEmpty()){
                        continue;
                    }
                    File file = new File(absolutePath);
                    if (file.exists() && !TextUtils.isEmpty(file.getName()) && file.getName().contains(".")) {
                        getOrWriteList(new FileBean(file.getName(), file.getPath()), operateType.SET);
                        currentSize++;
                        if(maxCnt > 0 && maxCnt <= currentSize){
                            break;
                        }
                    }
                }
                cursor.close();
            }
            Log.i("searchLocalImageFile", "search time cost " + (System.currentTimeMillis() - time));
            if(null != mListenr){
               mListenr.onSearchFinish();
            }
            isFinished = true;
        }
    }

    public void searchLocalImageFile(Context context){
        currentSize = 0;
        hasStarted = true;
        new searchLocalImageFileThread().start();
    }

    private void addLocalTxtFile(File[] files){
        if(maxCnt > 0 && maxCnt <= currentSize){
            return;
        }
        for(File file : files){
            if(file.isDirectory()){
                File[] filelist = file.listFiles();
                if(filelist != null){
                    addLocalTxtFile(filelist);
                }
            }else if (file.isFile()){
                String filename = file.getName();
                if(filename.endsWith(".txt") && file.length() > 10 * 1024){ //过滤条件txt结尾，且体积大于10KB
                    getOrWriteList(new FileBean(filename, file.getPath()), operateType.SET);
                    currentSize++;
                }
            }
        }
    }

    class searchLocalTxtFileThread extends Thread{//继承Thread类
        @Override
        public void run(){
            isFinished = false;
            long time = System.currentTimeMillis();
            File sdDir = Environment.getExternalStorageDirectory();
            addLocalTxtFile(new File[]{sdDir});
            Log.i("searchLocalTxtFile", "search time cost " + (System.currentTimeMillis() - time));
            if(null != mListenr){
                mListenr.onSearchFinish();
            }
            isFinished = true;
        }
    }
    public void searchLocalTxtFile(){
        currentSize = 0;
        hasStarted = true;
        new searchLocalTxtFileThread().start();
    }
}
