package com.github.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.camera.msCameraView.CameraView;
import com.github.camera.util.PermissionHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CameraActivity extends AppCompatActivity {

    public static final String RETURNED_IMAGE_ROTATION = "returned image rotation";
    public static final String DEFAULT_ACTION = "ms.camera.ACTION_IMAGE_CAPTURE";
    public static final String EXTRA_OUTPUT = "extra output";
    public final int finalWidth = 768;
    public final int finalHeight = 1280;

    private int[] flashStatusIcons = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_on,
            R.drawable.ic_flash_off
    };

    private int mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;

    private Bitmap photoTakenBitmap;

    private MediaPlayer mediaPlayer;

    private Uri photoTakenUri = null;

    private byte[] photoTakenByteData = null;

    private Handler mBackgroundHandler;

    FrameLayout rootLayout;

    ProgressBar progressBar;

    LinearLayout submitLayer;

    Button submitPhotoBt;

    Button cancelPhotoBt;

    ImageView takenPhotoView;

    ImageView takePhotoBt;

    ImageView changeFlashStatusBt;

    ImageView switchCameraFaceBt;

    CameraView cameraView;

    boolean hasCamera;

    boolean hasAutoFocus;

    boolean hasFlash;

    boolean hasFrontCamera;

    private Handler mHandler;

    private Handler permissionHandler;

    private PackageManager packageManager;

    private final int DEFAULT_MIN_WIDTH_QUALITY = 768;        // min pixels
    private final int DEFAULT_MIN_HEIGHT_QUALITY = 1024;        // min pixels


    private CameraView.Callback mCallback
            = new CameraView.Callback() {

        @Override
        public void onCameraOpened(CameraView cameraView) {
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
        }

        @Override
        public void onPictureTaken(final CameraView cameraView, final byte[] data) {

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                if(cameraView!=null){
                    cameraView.stop();
                }
                compressSaveAndShowPicture(data);
                }
            });
        }

    };

    private void compressSaveAndShowPicture(byte[] data){
        if(data==null || photoTakenUri==null){
            return;
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data,0,data.length,options);
        int maxHeight = DEFAULT_MIN_HEIGHT_QUALITY;
        int maxWidth = DEFAULT_MIN_WIDTH_QUALITY;
        options.inSampleSize = calculateInSampleSize(options,maxWidth,maxHeight);
        options.inJustDecodeBounds = false;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            options.inPurgeable = true;
        }
        options.inMutable = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        System.gc();
        Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length,options);
        File file = new File(photoTakenUri.getPath()+String.valueOf(System.currentTimeMillis())+".jpg");
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(file);
            if(bitmap!=null){
                bitmap.compress(Bitmap.CompressFormat.JPEG,80,outputStream);
            }
            outputStream.flush();
            outputStream.close();
            outputStream = null;
        }catch (IOException e){
            e.printStackTrace();
            outputStream = null;
        }

        ExifInterface exif;
        try {
            exif = new ExifInterface(file.getAbsolutePath());
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 0);
            if(orientation!=0){
                Matrix matrix = new Matrix();
                if (orientation == 6) {
                    matrix.postRotate(90);
                } else if (orientation == 3) {
                    matrix.postRotate(180);
                } else if (orientation == 8) {
                    matrix.postRotate(270);
                }
                if(bitmap!=null) {
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix,true);
                    FileOutputStream mOutputStream;
                    try {
                        mOutputStream = new FileOutputStream(file);
                        bitmap.compress(Bitmap.CompressFormat.JPEG,75,mOutputStream);
                        bitmap.recycle();
                        mOutputStream.flush();
                        mOutputStream.close();
                        mOutputStream = null;
                        System.gc();
                    }catch (IOException e){
                        e.printStackTrace();
                        bitmap.recycle();
                        mOutputStream = null;
                        System.gc();
                    }
                }
                exif = null;
            }else {
                if(bitmap!=null){
                    bitmap.recycle();
                    System.gc();
                }
            }

        }catch (IOException e){
            e.printStackTrace();
        }

        photoTakenUri = Uri.fromFile(file);
        file = null;
        takenPhotoView.setImageURI(null);
        takenPhotoView.setImageURI(photoTakenUri);
        takenPhotoView.setVisibility(View.VISIBLE);
        submitLayer.setVisibility(View.VISIBLE);
        changeFlashStatusBt.setVisibility(View.GONE);
        switchCameraFaceBt.setVisibility(View.GONE);
        takePhotoBt.setVisibility(View.GONE);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate ratios of height and width to requested height and width
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);

            // Choose the smallest ratio as inSampleSize value, this will guarantee
            // a final image with both dimensions larger than or equal to the
            // requested height and width.
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }


    public void switchCameraFaceClicked(){
        if(cameraView==null || !hasFrontCamera)
            return;
        cameraView.setFacing(cameraView.getFacing()==CameraView.FACING_BACK ? CameraView.FACING_FRONT : CameraView.FACING_BACK);
        if(hasAutoFocus) {
            cameraView.setAutoFocus(true);
        }
        toggleFlashButtonEnable(cameraView.getFacing()==CameraView.FACING_BACK);
    }

    private void toggleFlashButtonEnable(boolean enable) {
        changeFlashStatusBt.setEnabled(enable);
        changeFlashStatusBt.setClickable(enable);

        int resId=0;
        if(enable){
            if(mCurrentFlashStatusIcon==R.drawable.ic_auto_flash_disabled){
                resId = R.drawable.ic_flash_auto;
            }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_off_disabled){
                resId = R.drawable.ic_flash_off;
            }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_on_disabled){
                resId = R.drawable.ic_flash_on;
            }
        }else {
            if(mCurrentFlashStatusIcon==R.drawable.ic_flash_auto){
                resId = R.drawable.ic_auto_flash_disabled;
            }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_off){
                resId = R.drawable.ic_flash_off_disabled;
            }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_on){
                resId = R.drawable.ic_flash_on_disabled;
            }
        }
        if(resId!=0){
            changeFlashStatusBt.setImageResource(resId);
            mCurrentFlashStatusIcon = resId;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler(getMainLooper());
        checkCameraFeature();
        setContentView(R.layout.activity_camera);
        initFeatures();
        initViews();
        mediaPlayer = MediaPlayer.create(this,R.raw.camera_shutter_sound);
        if(!hasPermissions()) {
            requestPermissions(true);
        }else {
            initCamera();
            getIntentDate(getIntent());
        }
    }

    private void checkCameraFeature(){
        if(packageManager==null){
            packageManager = getPackageManager();
        }
        hasCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        if(!hasCamera){
            Toast.makeText(this,"دوربین دستگاه شما حداقل امکانات مورد نیاز را ندارد",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initFeatures(){
        if(packageManager==null){
            packageManager = getPackageManager();
        }
        hasAutoFocus = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_AUTOFOCUS);

        hasFlash = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);

        hasFrontCamera = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT);
    }

    private void initViews() {
        rootLayout = (FrameLayout)findViewById(R.id.camera_root_view);
        cameraView = (CameraView) findViewById(R.id.camera_view);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        submitLayer = (LinearLayout)findViewById(R.id.submit_layer);
        submitPhotoBt = (Button) findViewById(R.id.submit_photo_btn);
        cancelPhotoBt = (Button) findViewById(R.id.cancel_photo_btn);
        takenPhotoView = (ImageView)findViewById(R.id.photo_taken_view);
        takePhotoBt = (ImageView)findViewById(R.id.take_photo);
        changeFlashStatusBt = (ImageView)findViewById(R.id.flash_status);
        switchCameraFaceBt = (ImageView)findViewById(R.id.switch_face);

        submitPhotoBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitPhotoClicked();
            }
        });

        cancelPhotoBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancelPhotoClicked();
            }
        });

        takePhotoBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhotoClicked();
            }
        });

        changeFlashStatusBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeFlashStatusClicked();
            }
        });

        switchCameraFaceBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchCameraFaceClicked();
            }
        });

        toggleCameraSwitchButtonEnabled(hasFrontCamera);
    }

    private void toggleCameraSwitchButtonEnabled(boolean hasFrontCamera) {
        switchCameraFaceBt.setClickable(hasFrontCamera);
        switchCameraFaceBt.setEnabled(hasFrontCamera);
        switchCameraFaceBt.setImageResource(hasFrontCamera? R.drawable.camera_switch : R.drawable.camera_switch_disabled);
    }

    public void submitPhotoClicked(){
        if(photoTakenUri==null){
            return;
        }
        Intent output = new Intent();
        output.setData(photoTakenUri);
        setResult(RESULT_OK,output);
        photoTakenUri = null;
        finish();
    }

    /*private int calculateBitmapRotation(byte[] photoTakenByteData) {
        if(cameraView==null || photoTakenByteData==null)
            return 0;
        int rotation = 0;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(photoTakenByteData, 0, photoTakenByteData.length, options);
        if(cameraView.getFacing()==CameraView.FACING_BACK){
            if(options.outWidth>options.outHeight){
                rotation = 90;
            }
        }else{
            if(options.outWidth>options.outHeight){
                rotation = 270;
            }
        }
        return rotation;
    }*/

    /*private int getDisplayHeight(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.heightPixels;
    }

    private int getDisplayWidth(){
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        return displayMetrics.widthPixels;
    }*/

    /*private boolean isPictureOutOfDisplayBounds(byte[] data){
        if(cameraView==null || data==null)
            return false;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        // First decode with inJustDecodeBounds=true to check dimensions
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);
        if(options.outWidth>getDisplayWidth() || options.outHeight>getDisplayHeight()){
            return true;
        }else {
            return false;
        }
    }*/

    public void cancelPhotoClicked(){
        submitLayer.setVisibility(View.GONE);
        takenPhotoView.setImageBitmap(null);
        takenPhotoView.setVisibility(View.GONE);
        if(photoTakenByteData!=null){
            photoTakenByteData = null;
        }
        if(photoTakenBitmap!=null){
            photoTakenBitmap.recycle();
            System.gc();
        }
        changeFlashStatusBt.setVisibility(View.VISIBLE);
        switchCameraFaceBt.setVisibility(View.VISIBLE);
        takePhotoBt.setVisibility(View.VISIBLE);
        if(!hasPermissions()) {
            requestPermissions(false);
        }else {
            if (cameraView != null) {
                try {
                    cameraView.start();
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(this,"دوربین دستگاه شما حداقل امکانات مورد نیاز را ندارد",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    synchronized public void takePhotoClicked(){
        if(cameraView==null)
            return;
        if(!hasPermissions()) {
            requestPermissions(false);
        }else {
            if(mediaPlayer!=null){
                mediaPlayer.start();
            }
            cameraView.takePicture();
        }
    }

    public void changeFlashStatusClicked(){

        if(cameraView==null || !hasFlash)
            return;
        if(mCurrentFlashStatusIcon==R.drawable.ic_flash_auto){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_on;
            cameraView.setFlash(CameraView.FLASH_ON);
        }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_on){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_off;
            cameraView.setFlash(CameraView.FLASH_OFF);
        }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_off){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;
            cameraView.setFlash(CameraView.FLASH_AUTO);
        }else {
            mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;
            cameraView.setFlash(CameraView.FLASH_AUTO);
        }
        changeFlashStatusBt.setImageResource(mCurrentFlashStatusIcon);
        changeFlashStatusBt.setTag(mCurrentFlashStatusIcon);

    }

    synchronized private void getIntentDate(Intent intent) {
        if(intent==null){
            setResult(RESULT_CANCELED);
            finish();
        }else {
            String stringUri = intent.getStringExtra(EXTRA_OUTPUT);
            if(stringUri!=null && !stringUri.isEmpty()){
                photoTakenUri = Uri.parse(stringUri);
            }
        }
    }

    private void requestPermissions(final boolean isCreated) {
        PermissionHelper.RequestRuntimePermission(this, new String[]{
                PermissionHelper.CAMERA_PERMISSION,
                PermissionHelper.READ_STORAGE_PERMISSION,
                PermissionHelper.WRITE_STORAGE_PERMISSION},
                PermissionHelper.REQUEST_CODE_PERMISSION_CAMERA);
        if(permissionHandler==null){
            permissionHandler = new Handler(getMainLooper());
        }
        permissionHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(CameraActivity.this.isFinishing())
                    return;
                if(hasPermissions()){
                    if(!isCreated){
                        if (cameraView != null) {
                            try {
                                cameraView.start();
                            }catch (Exception e){
                                e.printStackTrace();
                                Toast.makeText(CameraActivity.this,"دوربین دستگاه شما حداقل امکانات مورد نیاز را ندارد",Toast.LENGTH_LONG).show();
                                finish();
                            }
                        }
                    }else{
                        initCamera();
                        getIntentDate(getIntent());
                    }
                }else {
                    if(permissionHandler!=null) {
                        permissionHandler.postDelayed(this, 1000);
                    }
                }
            }
        },1000);
    }

    private boolean hasPermissions(){
        return PermissionHelper.PermissionGranted(this, PermissionHelper.CAMERA_PERMISSION)
                && PermissionHelper.PermissionGranted(this, PermissionHelper.READ_STORAGE_PERMISSION)
                && PermissionHelper.PermissionGranted(this,PermissionHelper.WRITE_STORAGE_PERMISSION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(!hasPermissions()) {
            requestPermissions(false);
        }else {
            if (cameraView != null) {
                try {
                    cameraView.start();
                }catch (Exception e){
                    e.printStackTrace();
                    Toast.makeText(this,"دوربین دستگاه شما حداقل امکانات مورد نیاز را ندارد",Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    private void initCamera() {
        if(cameraView==null)
            return;
        try {
            cameraView.start();
        }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(this,"دوربین دستگاه شما حداقل امکانات مورد نیاز را ندارد",Toast.LENGTH_LONG).show();
            finish();
        }
        if(!hasFlash){
            toggleFlashButtonEnable(false);
        }else {
            cameraView.setFlash(CameraView.FLASH_AUTO);
        }
        if(hasAutoFocus) {
            cameraView.setAutoFocus(true);
        }
        cameraView.addCallback(mCallback);
    }

    private Uri getImageUri(Bitmap inImage, Context context) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Document", null);
        if(!inImage.isRecycled())
            inImage.recycle();
        return Uri.parse(path);
    }

    private Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            if(!bm.isRecycled())
                bm.recycle();
            return bmOut;
        }
        return bm;
    }

    public static Bitmap rotateBitmap(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            if(!bm.isRecycled())
                bm.recycle();
            return bmOut;
        }
        return bm;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        getIntentDate(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        packageManager = null;
        cameraView.releaseResources();
        mCallback = null;
        mHandler = null;
        permissionHandler = null;
        submitLayer.removeAllViews();
        rootLayout.removeAllViews();
        cameraView = null;
        progressBar = null;
        submitPhotoBt = null;
        cancelPhotoBt = null;
        takenPhotoView = null;
        takePhotoBt = null;
        changeFlashStatusBt = null;
        switchCameraFaceBt = null;
        submitLayer = null;
        rootLayout = null;
        flashStatusIcons = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraView!=null) {
            cameraView.stop();
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    private Handler getBackgroundHandler() {
        if (mBackgroundHandler == null) {
            HandlerThread thread = new HandlerThread("background");
            thread.start();
            mBackgroundHandler = new Handler(thread.getLooper());
        }
        return mBackgroundHandler;
    }
}
