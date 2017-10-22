package com.github.camera.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by Arash Golmohammadi (Arash.Golmohammadi@snapp.cab) on 4/5/2017.
 */


public class PermissionHelper {

    public static final int REQUEST_CODE_PERMISSION_CAMERA = 50001;

    public static String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    public static String WRITE_STORAGE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public static String READ_STORAGE_PERMISSION = Manifest.permission.READ_EXTERNAL_STORAGE;

    public static final int REQUEST_CODE_PERMISSION_READ_STORAGE = 50002;

    public static final int REQUEST_CODE_PERMISSION_WRITE_STORAGE = 50003;


    public static boolean PermissionGranted(Context context, String permission){

        int permissionCheck = ContextCompat.checkSelfPermission(context,permission);

        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            return true;
        }else{
            return false;
        }
    }

    public static void RequestRuntimePermission(Activity activity, String[] permissions, int permissionCode){
        if(permissions == null){
            throw new IllegalArgumentException();
        }
        ActivityCompat.requestPermissions(activity,permissions,permissionCode);
    }

}
