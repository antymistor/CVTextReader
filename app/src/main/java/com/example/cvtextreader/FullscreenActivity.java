package com.example.cvtextreader;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import android.util.Pair;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.utils.EyeDetector2;
import com.example.utils.EyeDetectorbase;
import com.example.utils.TextViewCreater;
import com.example.utils.TextViewAdvance;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import com.example.utils.EyeDetector;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class FullscreenActivity extends AppCompatActivity {
    public class statusinfo{
        float progress = 0;
    }
    private FrameLayout parentlayout  = null;
    private FrameLayout baselayout  = null;
    private SeekBar     progressbar = null;
    private statusinfo statusinfo_ = null;
    private Float lastpageprocess = 0.0f;
    private Float nextpageprocess = 0.0f;
    File progressfile;
    EyeDetectorbase detector;
    String FilePath;
    ArrayList<Pair<String, Float>> titlelist;
    long linesum = 0;
    Context mContext;
    Bitmap fakeFrame;
    TextViewAdvance viewtest;
    boolean isfirstload = true;

    ImageView fakeview;
    String fakepicpath = Environment.getExternalStorageDirectory().getPath() + "/DCIM/frame.png";
    private void saveFakeFrame(){
        if(viewtest != null) {
            viewtest.setDrawingCacheEnabled(true);
            viewtest.buildDrawingCache();
            fakeFrame = viewtest.getDrawingCache();
            try {
                fakeFrame.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(
                        new File(fakepicpath)));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            viewtest.destroyDrawingCache();
        }
    }
    private void showFakeFrame(){
        File file = new File(fakepicpath);
        if(file.exists()) {
            Bitmap map = BitmapFactory.decodeFile(fakepicpath);
            BitmapDrawable dr = new BitmapDrawable(map);
            parentlayout = findViewById(R.id.parentlayout);
            fakeview = new ImageView(this);
            fakeview.setBackground(dr);
            fakeview.setVisibility(View.VISIBLE);
            parentlayout.addView(fakeview);
            fakeview.bringToFront();
            Log.e("showFakeFrame","showFakeFrame");
        }
    }

    enum FakeFrameSaveStatus{
        has_saved,
        need_save_but_do_not_save,
        need_save_now
    }
    FakeFrameSaveStatus fakeFrameSaveStatus = FakeFrameSaveStatus.has_saved;

    private static boolean isJson(String content) {
        JsonElement jsonElement;
        try {
            jsonElement = new JsonParser().parse(content);
        } catch (Exception e) {
            return false;
        }
        if (jsonElement == null) {
            return false;
        }
        if (!jsonElement.isJsonObject()) {
            return false;
        }
        return true;
    }

    TextView pageshow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isfirstload = true;
        mContext = this;
        Objects.requireNonNull(getSupportActionBar()).hide();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            int flagTranslucentStatus = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
            int flagTranslucentNavigation = WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.flags |= flagTranslucentNavigation;
                window.setAttributes(attributes);
                getWindow().setStatusBarColor(Color.TRANSPARENT);
            } else {
                Window window = getWindow();
                WindowManager.LayoutParams attributes = window.getAttributes();
                attributes.flags |= flagTranslucentStatus | flagTranslucentNavigation;
                window.setAttributes(attributes);
            }
        }
        setContentView(R.layout.activity_fullscreen);
        showFakeFrame();
        baselayout = findViewById(R.id.baselayout);
        progressbar = findViewById(R.id.processbar);
        FilePath = getIntent().getStringExtra("filePath");

        //get and set status from disk
        try {
            progressfile = new File(Environment.getExternalStorageDirectory().getPath() + "/DCIM/progress.json");
            if (!progressfile.exists() && !progressfile.createNewFile()) {
                progressfile = null;
            } else {
                FileInputStream fin = new FileInputStream(progressfile);
                int len = fin.available();
                if (len > 0) {
                    byte[] buffer = new byte[len];
                    fin.read(buffer);
                    Gson gsonin = new Gson();
                    String jstr = new String(buffer);
                    if(isJson(jstr)) {
                        statusinfo_ = gsonin.fromJson(jstr, statusinfo.class);
                        progressbar.setProgress((int) (statusinfo_.progress * 100));
                        Log.e("aizhiqiang","read json success" + String.valueOf(statusinfo_.progress) );
                    }else{
                        Log.e("aizhiqiang","read json fail");
                    }
                }
            }
            if (statusinfo_ == null) {
                Log.e("aizhiqiang","read json fail");
                statusinfo_ = new statusinfo();
                statusinfo_.progress = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //build TextView
        TextViewCreater.TextViewPara para = new TextViewCreater.TextViewPara();
        para.txtpath = FilePath;
        para.basecontext = this;
        para.fontsize = 16;
        para.listener = new TextViewAdvance.IProcessListener() {
            @Override
            public void onProcess(float progress) {
                if (statusinfo_ != null) {
                    statusinfo_.progress = progress;
                    progressbar.setProgress((int) (statusinfo_.progress * 100));
                    fakeFrameSaveStatus = FakeFrameSaveStatus.need_save_but_do_not_save;
                    fakeview.setVisibility(View.GONE);
                }
            }
            @Override
            public void onToEnd() {
                Log.e("cvtextreader", "Has Move to end");
            }

            @Override
            public void onGetList(ArrayList<Pair<String, Float>> list, long linesum_) {
                titlelist = list;
                linesum = linesum_;
                //fakeview.setVisibility(View.GONE);
            }
        };
        viewtest = TextViewCreater.createTextView(para);
        baselayout.addView(viewtest);

        //set progressbar callback
        progressbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    statusinfo_.progress = 1.0f * progress / 100;
                    viewtest.seektopos(statusinfo_.progress);
                    fakeFrameSaveStatus = FakeFrameSaveStatus.need_save_but_do_not_save;
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        progressbar.bringToFront();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                //write current progress to disk
                if(progressfile.exists() && statusinfo_ != null){
                    try {
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(progressfile,false), "UTF-8"));
                        Gson gson = new Gson();
                        String content = gson.toJson(statusinfo_);
                        writer.write(content);
                        writer.close();
                    }catch ( IOException e) {
                        e.printStackTrace();
                    }
                }
                //write current fake frame to disk
                if(fakeFrameSaveStatus == FakeFrameSaveStatus.need_save_but_do_not_save){
                    fakeFrameSaveStatus = FakeFrameSaveStatus.need_save_now;
                }else if(fakeFrameSaveStatus == FakeFrameSaveStatus.need_save_now){
                    saveFakeFrame();
                    fakeFrameSaveStatus = FakeFrameSaveStatus.has_saved;
                    Log.e("saveFakeFrame", "saveFakeFrame now !!");
                }
                //show page title by toast
                if(titlelist != null && titlelist.size() > 0 && linesum > 0 && statusinfo_!= null  &&
                  (statusinfo_.progress * linesum >= nextpageprocess || statusinfo_.progress * linesum < lastpageprocess)){
                    for(int i = 0 ; i != titlelist.size() ; ++i){
                        if( 1.0f * titlelist.get(i).second / linesum < statusinfo_.progress &&
                          ((i + 1) == titlelist.size() || 1.0f * titlelist.get(i + 1).second / linesum > statusinfo_.progress)){
                                lastpageprocess = titlelist.get(i).second;
                                if((i + 1) == titlelist.size()){
                                    nextpageprocess = (float)linesum;
                                }else{
                                    nextpageprocess = titlelist.get(i+1).second;
                                }
                                int finalI = i;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        pageshow.setText(titlelist.get(finalI).first);
                                    }
                                });
                                Log.e("aizhiqiang", "current page is" + titlelist.get(i).first);
                                break;
                        }
                    }
                }
            }
        }, 100, 100);

        //set textview to last finished position
        viewtest.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(statusinfo_ != null && isfirstload) {
                    Log.e("aizhiqiang", "first set progress" + String.valueOf(statusinfo_.progress));
                    viewtest.seektopos(statusinfo_.progress);
                    isfirstload = false;
                }
            }
        });

        //Enable 启EyeDetector by eyeswitch status
        ((CheckBox) findViewById(R.id.eyeswitch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    if(detector == null){
                        EyeDetector.EyeDetectorStatus intitpara = new EyeDetector.EyeDetectorStatus();
                        intitpara.inputframesize = new Size(720, 1280);
                        intitpara.maxfacecnt = 1;
                        detector = new EyeDetector2(intitpara);
                        detector.setListener(new EyeDetectorbase.IEyeStatusListener() {
                            @Override
                            public void onEvent(EyeDetectorbase.eyeEvent event) {
                                if (event == EyeDetectorbase.eyeEvent.EyeAllOpen) {
                                    viewtest.enableEyeAutoMoving(TextViewAdvance.AutoMovingMode.None);
                                    Log.e("eyestatus", "eyestatus changed --> EyeOpen");
                                } else if (event == EyeDetectorbase.eyeEvent.LeftEyeClose) {
                                    viewtest.enableEyeAutoMoving(TextViewAdvance.AutoMovingMode.Down);
                                    Log.e("eyestatus", "eyestatus changed --> LeftEyeClose");
                                } else if (event == EyeDetectorbase.eyeEvent.RightEyeClose) {
                                    viewtest.enableEyeAutoMoving(TextViewAdvance.AutoMovingMode.UP);
                                    Log.e("eyestatus", "eyestatus changed --> RightEyeClose");
                                }
                            }
                        });
                    }
                }else{
                    if(detector != null){
                        detector.destroy();
                        detector = null;
                    }
                    viewtest.enableEyeAutoMoving(TextViewAdvance.AutoMovingMode.None);
                }
            }
        });
        ((CheckBox) findViewById(R.id.eyeswitch)).bringToFront();

        pageshow = findViewById(R.id.pageshow);
        pageshow.setTextSize(13);
        pageshow.setGravity(Gravity.CENTER);
        pageshow.setTextColor(Color.parseColor("#000000"));
    }



}