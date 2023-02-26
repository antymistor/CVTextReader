package com.example.cvtextreader;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

import android.util.Size;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.SeekBar;

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
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import com.example.utils.EyeDetector;
//import com.example.cvtextreader.databinding.ActivityFullscreenBinding;

public class FullscreenActivity extends AppCompatActivity {
    public class statusinfo{
        float progress = 0;
    }
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 0x01;
    private static final int REQUEST_CAMERA_CODE = 0x100;
    private FrameLayout baselayout  = null;
    private SeekBar     progressbar = null;
    private statusinfo statusinfo_ = null;
    File progressfile;
    EyeDetectorbase detector;
    String FilePath;
    TimerTask processtask = new TimerTask() {
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
    };
    private Boolean CheckPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // 安卓11，判断有没有“所有文件访问权限”权限
            if (Environment.isExternalStorageManager()) {
                return true;
            } else {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                return false;
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_CODE);
            }
        }
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.parseColor("#bb9977"));

        setContentView(R.layout.activity_fullscreen);
        baselayout = findViewById(R.id.baselayout);
        progressbar = findViewById(R.id.processbar);
        FilePath = getIntent().getStringExtra("filePath");
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
        };
        TextViewAdvance viewtest = TextViewCreater.createTextView(para);
        baselayout.addView(viewtest);
        progressbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    statusinfo_.progress = 1.0f * progress / 100;
                    viewtest.seektopos(statusinfo_.progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        new Timer().schedule(processtask, 1000, 5000);
        viewtest.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                viewtest.seektopos(statusinfo_.progress);
            }
        });
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
                        viewtest.enableEyeAutoMoving(TextViewAdvance.AutoMovingMode.None);
                        detector.destroy();
                        detector = null;
                    }
                }
            }
        });
    }

}