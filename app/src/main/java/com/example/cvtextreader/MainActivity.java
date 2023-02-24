package com.example.cvtextreader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by aizhiqiang on 2023/2/24
 *
 * @author aizhiqiang@bytedance.com
 */
public class MainActivity extends AppCompatActivity {
    public void goNextpage(){
        Intent intent = new Intent(this, FullscreenActivity.class);
        intent.putExtra("filePath", Environment.getExternalStorageDirectory().getPath() +"/DCIM/cache.txt");
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().setStatusBarColor(Color.parseColor("#dadada"));
        setContentView(R.layout.activity_main);
        findViewById(R.id.button).setBackgroundColor(Color.parseColor("#dadada"));
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
                File cache = new File(Environment.getExternalStorageDirectory().getPath() +"/DCIM/cache.txt");
                FileOutputStream fos = new FileOutputStream(cache);
                FileUtils.copy(is, fos);
                fos.close();
                is.close();
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
