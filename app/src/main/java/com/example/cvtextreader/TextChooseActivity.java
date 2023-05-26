package com.example.cvtextreader;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.FileUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.example.utils.SearchFileProvider;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by aizhiqiang on 2023/5/25
 *
 * @author aizhiqiang@bytedance.com
 */

public class TextChooseActivity extends AppCompatActivity {
    ListView mlist;
    private List<String> mNameList;
    private List<String> mPathList;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        attributes.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        getWindow().setAttributes(attributes);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_choose);
        mlist = findViewById(R.id.mylistview);
        mNameList = new ArrayList<String>();
        mPathList = new ArrayList<String>();
        ArrayAdapter<String> adapter = new ArrayAdapter<String> (TextChooseActivity.this, R.layout.text_item, mNameList);
        mlist.setAdapter(adapter);
        ArrayList<SearchFileProvider.FileBean> li =
                SearchFileProvider.getInstance(null).getOrWriteList(null, SearchFileProvider.operateType.READ);
        for(SearchFileProvider.FileBean f : li){
            mNameList.add(f.fileName.substring(0, f.fileName.length() - 4));
            mPathList.add(f.filePath);
        }
        adapter.notifyDataSetChanged();
        if(!SearchFileProvider.getInstance(null).isFinished()){
            if(!SearchFileProvider.getInstance(null).hasStarted()){
                SearchFileProvider.getInstance(null).setMaxCnt(15);
                SearchFileProvider.getInstance(null).searchLocalTxtFile();
            }
            SearchFileProvider.getInstance(null).setLisenter(new SearchFileProvider.ISearchListener() {
                @Override
                public void onSearchFinish() {}
                @Override
                public void onSearchFile(SearchFileProvider.FileBean f) {
                   runOnUiThread(() -> {
                       mNameList.add(f.fileName.substring(0, f.fileName.length() - 4));
                       mPathList.add(f.filePath);
                       adapter.notifyDataSetChanged();
                   });
                }
            });
        }
        mlist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    FileInputStream is = new FileInputStream(new File(mPathList.get(position)));
                    FileOutputStream fos = openFileOutput("cache.txt", 0);
                    FileUtils.copy(is, fos);
                    fos.close();
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(TextChooseActivity.this, FullscreenActivity.class);
                intent.putExtra("filePath", getFilesDir().getPath() +"/cache.txt");
                intent.putExtra("fileName", mNameList.get(position));
                startActivity(intent);
            }
        });
    }

}
