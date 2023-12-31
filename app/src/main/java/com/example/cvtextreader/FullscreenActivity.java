package com.example.cvtextreader;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;

import android.util.Pair;
import android.util.Size;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.utils.EyeDetector2;
import com.example.utils.EyeDetectorbase;
import com.example.utils.SeekbarAdvance;
import com.example.utils.TextViewCreater;
import com.example.utils.TextViewAdvance;
import com.example.utils.ViewFlingerAdvance;
import com.example.utils.ImageViewAdvance;
import com.google.gson.Gson;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.example.utils.EyeDetector;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.example.utils.Permission;

public class FullscreenActivity extends AppCompatActivity {
    public class statusinfo{
        ConcurrentHashMap<String, Float> fileprogresslist = new ConcurrentHashMap<>();
        String currentFileNameDy = "";
        float progress = 0;
        boolean islightmode = true;
    }
    private FrameLayout parentlayout   = null;
    private FrameLayout baselayout     = null;
    private SeekbarAdvance progressbar = null;
    private statusinfo statusinfo_     = null;
    private Float lastpageprocess      = 0.0f;
    private Float nextpageprocess      = 0.0f;
    File progressfile;
    EyeDetectorbase detector;
    String FilePath;
    String preFileName = "";
    ArrayList<Pair<String, Float>> titlelist;
    long linesum = 0;
    Context mContext;
    Bitmap fakeFrame;
    TextViewAdvance viewtest;
    Timer mtimer;
    int pageindex = 0;
    boolean isfirstload = true;
    static final int ProcessBarMax = 2000;
    ImageView fakeview;
    String fakepicpath = "";
    TextView pageshow;
    boolean isEyeCtrON = false;
    ViewFlingerAdvance flinger;
    ImageViewAdvance imageViewBack;
    Float maxprogress = 1.0f;
    Boolean hasLoadDisk = false;
    private void saveFakeFrame(){
        if(viewtest != null) {
            imageViewBack.setDrawingCacheEnabled(true);
            imageViewBack.buildDrawingCache();
            Bitmap backBitmap = imageViewBack.getDrawingCache();
            viewtest.setDrawingCacheEnabled(true);
            viewtest.buildDrawingCache();
            Bitmap frontBitmap = viewtest.getDrawingCache();

            Canvas canvas = new Canvas(backBitmap);
            Rect baseRect  = new Rect(0, 0, backBitmap.getWidth(), backBitmap.getHeight());
            Rect frontRect = new Rect(0, 0, frontBitmap.getWidth(), frontBitmap.getHeight());
            canvas.drawBitmap(frontBitmap, frontRect, baseRect, null);
            fakeFrame = backBitmap;
            try {
                fakeFrame.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(fakepicpath));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            viewtest.destroyDrawingCache();
            imageViewBack.destroyDrawingCache();
        }
    }
    private void showFakeFrame(){
        File file = new File(fakepicpath);
        if(file.exists()) {
            parentlayout = findViewById(R.id.parentlayout);
            fakeview = new ImageView(this);
            fakeview.setBackground( new BitmapDrawable(BitmapFactory.decodeFile(fakepicpath)));
            fakeview.setVisibility(View.VISIBLE);
            parentlayout.addView(fakeview);
            fakeview.bringToFront();
            Log.e("showFakeFrame","showFakeFrame");
        }
    }

    private void setlightmode(){
        if(viewtest!=null && pageshow!=null) {
            if (statusinfo_.islightmode) {
                viewtest.setTextColor(Color.parseColor("#bb554444"));
                pageshow.setBackgroundColor(Color.parseColor("#cce1ccaa"));
                pageshow.setTextColor(Color.parseColor("#000000"));
                imageViewBack.setOriBitmapResid(R.drawable.backlight);
                imageViewBack.loadbitmap();
            } else {
                viewtest.setTextColor(Color.parseColor("#555555"));
                pageshow.setBackgroundColor(Color.parseColor("#cc111111"));
                pageshow.setTextColor(Color.parseColor("#555555"));
                imageViewBack.setOriBitmapResid(R.drawable.backdark);
                imageViewBack.loadbitmap();
            }
        }
    }

    private void GetAndSetStatusFromDisk(){
        if(hasLoadDisk){
            return;
        }
        try {
            progressfile = new File(getFilesDir().getPath() + "/progress.json");
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
                        preFileName = statusinfo_.currentFileNameDy;
                        statusinfo_.currentFileNameDy = getIntent().getStringExtra("fileName").isEmpty() ? statusinfo_.currentFileNameDy : getIntent().getStringExtra("fileName");
                        fakepicpath = getFilesDir().getPath() + "/" + statusinfo_.currentFileNameDy + ".png";
                        if(statusinfo_.currentFileNameDy != null && statusinfo_.fileprogresslist != null && statusinfo_.fileprogresslist.containsKey(statusinfo_.currentFileNameDy) ){
                            Float pf =  statusinfo_.fileprogresslist.get(statusinfo_.currentFileNameDy);
                            if(pf != null){
                                statusinfo_.progress = pf;
                            }else{
                                statusinfo_.progress = 0.0f;
                            }
                        }
                        Log.e("antymistor","read json success" + String.valueOf(statusinfo_.progress) );
                    }else{
                        Log.e("antymistor","read json fail");
                    }
                }
            }
            if (statusinfo_ == null) {
                Log.e("antymistor","read json fail");
                statusinfo_ = new statusinfo();
                statusinfo_.progress = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        hasLoadDisk = true;
    }

    private void LoadViews(){
        // add imageview
        {
            imageViewBack = new ImageViewAdvance(this);
            baselayout.addView(imageViewBack);
            imageViewBack.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    imageViewBack.loadbitmap();
                }
            });
        }

        //add TextView
        {
            TextViewCreater.TextViewPara para = new TextViewCreater.TextViewPara();
            para.txtpath = FilePath;
            para.basecontext = this;
            para.fontsize = 16;
            para.listener = new TextViewAdvance.IProcessListener() {
                @Override
                public void onProcess(float progress) {
                    if (statusinfo_ != null) {
                        statusinfo_.progress = progress;
                        progressbar.setProgress((int) (statusinfo_.progress * ProcessBarMax));
                        fakeFrameSaveStatus = FakeFrameSaveStatus.need_save_but_do_not_save;
                        if(fakeview != null) {
                            fakeview.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onToEnd() {
                    maxprogress = statusinfo_.progress;
                    Log.e("cvtextreader", "Has Move to end");
                }

                @Override
                public void onGetList(ArrayList<Pair<String, Float>> list, long linesum_) {
                    titlelist = list;
                    linesum = linesum_;
                }
            };
            viewtest = TextViewCreater.createTextView(para);
            baselayout.addView(viewtest);
            viewtest.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (statusinfo_ != null && isfirstload) {
                        Log.e("antymistor", "first set progress" + String.valueOf(statusinfo_.progress));
                        viewtest.seektopos(statusinfo_.progress);
                        isfirstload = false;
                    }
                }
            });
        }

        //add flinger
        {
            flinger = new ViewFlingerAdvance(this);
            flinger.addScrollListener(new ViewFlingerAdvance.IScrollListener() {
                @Override
                public void scrollByHeight(int dy) {
                    if( (dy < 0 && statusinfo_.progress <=0) ||
                        (dy > 0 && statusinfo_.progress >=maxprogress) ){
                        return;
                    }
                    viewtest.constrainScrollBy(dy);
                    imageViewBack.constrainScrollBy(dy);
                }
            });
            baselayout.addView(flinger);
        }


        //add progressbar
        {
            progressbar.setMax(ProcessBarMax);
            progressbar.getThumb().setColorFilter(Color.parseColor("#ff00ff00"), PorterDuff.Mode.SRC_IN);
            progressbar.setOnClickThumbListener(new SeekbarAdvance.OnClickThumbListener() {
                @Override
                public void onClickThumb() {
                    isEyeCtrON = !isEyeCtrON;
                    if (isEyeCtrON) {
                        progressbar.getThumb().setColorFilter(Color.parseColor("#ffff0000"), PorterDuff.Mode.SRC_IN);
                        if (detector == null) {
                            EyeDetector.EyeDetectorStatus intitpara = new EyeDetector.EyeDetectorStatus();
                            intitpara.inputframesize = new Size(720, 1280);
                            intitpara.maxfacecnt = 1;
                            detector = new EyeDetector2(intitpara);
                            detector.setListener(new EyeDetectorbase.IEyeStatusListener() {
                                @Override
                                public void onEvent(EyeDetectorbase.eyeEvent event) {
                                    if (event == EyeDetectorbase.eyeEvent.EyeAllOpen) {
                                        flinger.enableEyeAutoMoving(ViewFlingerAdvance.AutoMovingMode.None);
                                        Log.e("eyestatus", "eyestatus changed --> EyeOpen");
                                    } else if (event == EyeDetectorbase.eyeEvent.LeftEyeClose) {
                                        flinger.enableEyeAutoMoving(ViewFlingerAdvance.AutoMovingMode.Down);
                                        Log.e("eyestatus", "eyestatus changed --> LeftEyeClose");
                                    } else if (event == EyeDetectorbase.eyeEvent.RightEyeClose) {
                                        flinger.enableEyeAutoMoving(ViewFlingerAdvance.AutoMovingMode.UP);
                                        Log.e("eyestatus", "eyestatus changed --> RightEyeClose");
                                    }
                                }
                            });
                        }
                    } else {
                        progressbar.getThumb().setColorFilter(Color.parseColor("#ff00ff00"), PorterDuff.Mode.SRC_IN);
                        if (detector != null) {
                            detector.destroy();
                            detector = null;
                        }
                        flinger.enableEyeAutoMoving(ViewFlingerAdvance.AutoMovingMode.None);
                    }
                }
            });
            progressbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        statusinfo_.progress = 1.0f * progress / ProcessBarMax;
                        viewtest.seektopos(statusinfo_.progress);
                        fakeFrameSaveStatus = FakeFrameSaveStatus.need_save_but_do_not_save;
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });
            progressbar.setProgress((int) (statusinfo_.progress * ProcessBarMax));
            progressbar.bringToFront();
        }

        //addpageshow
        {
            pageshow = findViewById(R.id.pageshow);
            pageshow.setTextSize(13);
            pageshow.setGravity(Gravity.CENTER);
            pageshow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (statusinfo_ != null) {
                        statusinfo_.islightmode = !statusinfo_.islightmode;
                        setlightmode();
                        viewtest.seektopos(statusinfo_.progress);
                    }
                }
            });
        }
    }

    private void LoadTimer(){
        mtimer = new Timer();
        mtimer.schedule(new TimerTask() {
            @Override
            public void run() {
                //write current progress to disk
                if(progressfile.exists() && statusinfo_ != null){
                    try {
                        if(statusinfo_.currentFileNameDy != null && !statusinfo_.currentFileNameDy.isEmpty()){
                            statusinfo_.fileprogresslist.put(statusinfo_.currentFileNameDy, statusinfo_.progress);
                        }
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(progressfile,false), StandardCharsets.UTF_8));
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
                            pageindex = i;
                            Log.e("antymistor", "current page is" + titlelist.get(i).first);
                            break;
                        }
                    }
                }
                String pagetext = "";
                if(titlelist != null && titlelist.size() > 0 && linesum > 0 && statusinfo_!= null){
                    pagetext += titlelist.get(pageindex).first;
                }
                String progresstext = "";
                if(statusinfo_ != null) {
                    progresstext =  new DecimalFormat( ".00" ).format(statusinfo_.progress * 100) + "%";
                }
                String finalPagetext = pagetext + " " + "<font color='#999999'>" + progresstext + "</font>";
                runOnUiThread(() -> pageshow.setText(Html.fromHtml(finalPagetext)));
            }
        }, 100, 100);
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

    @Override
    public void onBackPressed() {
         super.onBackPressed();//注释掉这行,back键不退出activity
        //release
         if(mtimer != null){
             mtimer.cancel();
             mtimer = null;
         }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isfirstload = true;
        mContext = this;
        GetAndSetStatusFromDisk();
        //fakepicpath = getFilesDir().getPath() + "/frame.png";

        //pretty layout
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        getWindow().setAttributes(attributes);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_fullscreen);
        showFakeFrame();

        //get para
        baselayout = findViewById(R.id.baselayout);
        progressbar = findViewById(R.id.processbar);
        FilePath = getIntent().getStringExtra("filePath");

        //load
        //GetAndSetStatusFromDisk();
        LoadViews();
        LoadTimer();
        setlightmode();
    }



}