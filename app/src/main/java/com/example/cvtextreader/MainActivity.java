package com.example.cvtextreader;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Created by aizhiqiang on 2023/2/24
 *
 * @author aizhiqiang@bytedance.com
 */
public class MainActivity extends AppCompatActivity {
    private FrameLayout parentlayout  = null;
    private FrameLayout baselayout  = null;
    private AppCompatImageView background;
    private String filename = "";
    private static int pageindex = 0;
    private int backviewWidth = 0;
    private int backviewHeight = 0;
    public void goNextpage(){
        Intent intent = new Intent(this, FullscreenActivity.class);
        intent.putExtra("filePath", getFilesDir().getPath() +"/cache.txt");
        intent.putExtra("fileName", filename);
        startActivity(intent);
        if(backviewWidth >0 && backviewHeight >0){
            loadBitmap(-1);
        }else{
            parentlayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if(backviewWidth <=0 || backviewHeight <=0){
                        backviewWidth = background.getWidth();
                        backviewHeight = background.getHeight();
                        loadBitmap(0);
                    }
                }
            });
        }
    }

    static List<Integer> backlistid = Arrays.asList(R.drawable.g0,
                                                    R.drawable.g1,
                                                    R.drawable.g2,
                                                    R.drawable.g3,
                                                    R.drawable.g4,
                                                    R.drawable.g5,
                                                    R.drawable.g6,
                                                    R.drawable.g7);
    private void loadBitmap(int index){
        if(backviewWidth <=0 || backviewHeight <=0){
            return;
        }
        pageindex = index >=0 ? index : (new Random().nextInt(backlistid.size()));
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), backlistid.get(pageindex), options);
        int sampleSize = (int)Math.floor((float) options.outWidth / 1440.f);
        options.inSampleSize = (int)Math.floor(Math.log(sampleSize)/Math.log(2));
        options.inJustDecodeBounds = false;
        Bitmap mapori = BitmapFactory.decodeResource(getResources(), backlistid.get(pageindex), options);
        float viewRatio = 1.0f * backviewWidth / backviewHeight;
        float mapRatio = 1.0f * mapori.getWidth() / mapori.getHeight();
        float scaleRatio = Math.min(viewRatio / mapRatio , 1.0f);
        Matrix mat = new Matrix();
        float scale = 1440.f /mapori.getWidth();
        mat.postScale(scale, scale);
        mapori = Bitmap.createBitmap(mapori, (int)(mapori.getWidth() * (1- scaleRatio) / 2), 0,
                                             (int)(mapori.getWidth() * scaleRatio), mapori.getHeight(),mat, true);

        background.setBackground( new BitmapDrawable(mapori));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        getWindow().setAttributes(attributes);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_main);
        parentlayout = findViewById(R.id.parentlayoutmain);
        background = new AppCompatImageView(this);
        parentlayout.addView(background);

        findViewById(R.id.button).setBackgroundColor(Color.parseColor("#00000000"));
        goNextpage();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            Uri uri = data.getData();
            try {
                InputStream is = getContentResolver().openInputStream(uri);
                FileOutputStream fos = openFileOutput("cache.txt", 0);
                FileUtils.copy(is, fos);
                fos.close();
                is.close();
                DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
                if(documentFile != null) {
                    filename = documentFile.getName();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            goNextpage();
        }
    }

    public void startread(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");//设置类型
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, 3);
    }

}
