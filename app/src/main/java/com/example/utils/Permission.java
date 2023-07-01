package com.example.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;

/**
 * Created by antymistor on 2023/3/2
 *
 * @author azq2018@zju.edu.cn
 */
public class Permission {
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_CAMERA_CODE = 100;
    public static final int REQUEST_CODE = 5;
    //定义三个权限
    private static final String[] permission = new String[]{
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };
    //每个权限是否已授
    public static boolean isPermissionGranted(Activity activity){
        // 安卓11，判断有没有“所有文件访问权限”权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivityForResult(intent, MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
        boolean sSRPR=ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)|
                      ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)|
                      ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA);
        if(!sSRPR){
            ActivityCompat.requestPermissions(activity,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
            },0);
        }
        return true;
    }

    public static boolean checkPermission(Activity activity){
       return isPermissionGranted(activity);
    }
}