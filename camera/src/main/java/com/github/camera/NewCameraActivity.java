package com.github.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.github.camera.util.PermissionHelper;
import com.google.android.cameraview.CameraView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NewCameraActivity extends AppCompatActivity {

    public static final String RETURNED_IMAGE_ROTATION = "returned image rotation";
    public static final String DEFAULT_ACTION = "ms.camera.ACTION_IMAGE_CAPTURE";
    public static final String EXTRA_OUTPUT = "extra output";

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

    ProgressBar progressBar;

    LinearLayout submitLayer;

    Button submitPhotoBt;

    Button cancelPhotoBt;

    ImageView takenPhotoView;

    ImageView takePhotoBt;

    ImageView changeFlashStatusBt;

    ImageView switchCameraFaceBt;

    CameraView cameraView;

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

            new Handler(getMainLooper()).post(new Runnable() {
                @Override
                public void run() {

                    photoTakenByteData = data;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    photoTakenBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    if(photoTakenBitmap!=null) {
                        photoTakenBitmap = cameraView.getFacing()==CameraView.FACING_BACK ?
                                rotate(photoTakenBitmap, 90):
                                rotate(photoTakenBitmap, 270);
                        takenPhotoView.setImageBitmap(photoTakenBitmap);
                        takenPhotoView.setVisibility(View.VISIBLE);
                        submitLayer.setVisibility(View.VISIBLE);
                        changeFlashStatusBt.setVisibility(View.GONE);
                        switchCameraFaceBt.setVisibility(View.GONE);
                        takePhotoBt.setVisibility(View.GONE);
                    }
                }
            });
        }

    };

    public void switchCameraFaceClicked(){
        if(cameraView==null)
            return;
        cameraView.setFacing(cameraView.getFacing()==CameraView.FACING_BACK ? CameraView.FACING_FRONT : CameraView.FACING_BACK);
        toggleFlashButtonEnable(cameraView.getFacing()==CameraView.FACING_BACK);
    }

    private void toggleFlashButtonEnable(boolean enable) {
        changeFlashStatusBt.setEnabled(enable);
        changeFlashStatusBt.setClickable(enable);

        if(cameraView!=null){
            int resId;
            switch (cameraView.getFlash()){
                case CameraView.FLASH_ON:
                    resId = enable ? R.drawable.ic_flash_on : R.drawable.ic_flash_on_disabled;
                    break;
                case CameraView.FLASH_OFF:
                    resId = enable ? R.drawable.ic_flash_off : R.drawable.ic_flash_off_disabled;
                    break;
                case CameraView.FLASH_AUTO:
                    resId = enable ? R.drawable.ic_flash_auto : R.drawable.ic_auto_flash_disabled;
                    break;
                default:
                    resId = enable ? R.drawable.ic_flash_auto : R.drawable.ic_auto_flash_disabled;
                    break;
            }
            changeFlashStatusBt.setImageResource(resId);
        }



    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_camera);
        initViews();
        mediaPlayer = MediaPlayer.create(this,R.raw.camera_shutter_sound);
        if(!hasPermissions()) {
            requestPermissions(true);
        }else {
            initCamera();
            getIntentDate(getIntent());
        }
    }

    private void initViews() {
        cameraView = (CameraView)findViewById(R.id.camera_view);
        progressBar = (ProgressBar)findViewById(R.id.progress);
        submitLayer = (LinearLayout)findViewById(R.id.submit_layer);
        submitPhotoBt = submitLayer.findViewById(R.id.submit_photo_btn);
        cancelPhotoBt = submitLayer.findViewById(R.id.cancel_photo_btn);
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
    }

    public void submitPhotoClicked(){
        if(photoTakenByteData!=null && photoTakenUri!=null && photoTakenUri.getPath()!=null){
            File file = new File(photoTakenUri.getPath());
            try{
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(photoTakenByteData);
                outputStream.flush();
                outputStream.close();
                Intent output = new Intent();
                output.putExtra(RETURNED_IMAGE_ROTATION,cameraView.getFacing()==CameraView.FACING_BACK ? 90 : 270);
                output.setData(Uri.fromFile(file));
                setResult(RESULT_OK,output);
                if(photoTakenBitmap!=null && !photoTakenBitmap.isRecycled()){
                    photoTakenBitmap.recycle();
                }
                finish();
            }catch (IOException e){
                e.printStackTrace();
            }


        }
    }
    public void cancelPhotoClicked(){
        submitLayer.setVisibility(View.GONE);
        takenPhotoView.setImageBitmap(null);
        takenPhotoView.setVisibility(View.GONE);
        if(photoTakenBitmap!=null && !photoTakenBitmap.isRecycled()){
            photoTakenBitmap.recycle();
        }
        changeFlashStatusBt.setVisibility(View.VISIBLE);
        switchCameraFaceBt.setVisibility(View.VISIBLE);
        takePhotoBt.setVisibility(View.VISIBLE);
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

        if(cameraView==null)
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
        PermissionHelper.RequestRuntimePermission(this, new String[]{PermissionHelper.CAMERA_PERMISSION,PermissionHelper.READ_STORAGE_PERMISSION,PermissionHelper.WRITE_STORAGE_PERMISSION}, PermissionHelper.REQUEST_CODE_PERMISSION_CAMERA);
        final Handler handler = new Handler(getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(NewCameraActivity.this.isFinishing())
                    return;
                if(hasPermissions()){
                    if(!isCreated){
                        if (cameraView != null) {
                            cameraView.start();
                        }
                    }else{
                        initCamera();
                        getIntentDate(getIntent());
                    }
                }else {
                    handler.postDelayed(this,1000);
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
                cameraView.start();
            }
        }
    }

    private void initCamera() {
        if(cameraView==null)
            return;
        cameraView.start();
        cameraView.addCallback(mCallback);
    }

    private Uri getImageUri(Bitmap inImage, Context context) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Document", null);
        //if(!inImage.isRecycled())inImage.recycle();
        return Uri.parse(path);
    }

    private Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            //if(!bm.isRecycled())bm.recycle();
            return bmOut;
        }
        return bm;
    }

    public static Bitmap rotateBitmap(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            //if(!bm.isRecycled())bm.recycle();
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraView!=null)
            cameraView.stop();
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
