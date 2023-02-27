package com.example.cvtextreader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import android.util.Pair;
import android.util.Size;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import com.example.utils.EyeDetector2;
import com.example.utils.EyeDetectorbase;
import com.example.utils.TextViewCreater;
import com.example.utils.TextViewAdvance;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import com.example.utils.EyeDetector;
//import com.example.cvtextreader.databinding.ActivityFullscreenBinding;

public class FullscreenActivity extends AppCompatActivity {
    public class statusinfo{
        float progress = 0;
    }
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.parseColor("#ccaa88"));

        setContentView(R.layout.activity_fullscreen);
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
                    statusinfo_ = gsonin.fromJson(new String(buffer), statusinfo.class);
                    progressbar.setProgress((int) (statusinfo_.progress * 100));
                }
            }
            if (statusinfo_ == null) {
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
        para.listener = new TextViewAdvance.IProcessListener() {
            @Override
            public void onProcess(float progress) {
                if (statusinfo_ != null) {
                    statusinfo_.progress = progress;
                    progressbar.setProgress((int) (statusinfo_.progress * 100));
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
            }
        };
        TextViewAdvance viewtest = TextViewCreater.createTextView(para);
        baselayout.addView(viewtest);

        //set progressbar callback
        progressbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    statusinfo_.progress = 1.0f * progress / 100;
                    viewtest.seektopos(statusinfo_.progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        //write current progress to disk
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
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
            }
        }, 1000, 5000);


        //show page title by toast
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if(titlelist != null && titlelist.size() > 0     &&
                         linesum > 0 && statusinfo_!= null       &&
                (statusinfo_.progress * linesum >= nextpageprocess ||
                statusinfo_.progress * linesum < lastpageprocess)){
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
                                    Toast toast = Toast.makeText(mContext, titlelist.get(finalI).first, Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            });
                            Log.e("aizhiqiang", "current page is" + titlelist.get(i).first);
                            break;
                        }
                    }
                }
            }
        }, 300, 100);


        //set textview to last finished position
        viewtest.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if(statusinfo_ != null ) {
                    viewtest.seektopos(statusinfo_.progress);
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
    }

}