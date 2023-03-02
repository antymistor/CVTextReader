package com.example.cvtextreader;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;
import android.view.WindowManager;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Created by aizhiqiang on 2023/2/24
 *
 * @author aizhiqiang@bytedance.com
 */
public class MainActivity extends AppCompatActivity {
    String filename = "";
    public void goNextpage(){
        Intent intent = new Intent(this, FullscreenActivity.class);
        intent.putExtra("filePath", getFilesDir().getPath() +"/cache.txt");
        intent.putExtra("fileName", filename);
        startActivity(intent);
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
