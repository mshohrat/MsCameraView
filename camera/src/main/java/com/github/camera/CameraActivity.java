package com.github.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import com.github.camera.util.PermissionHelper;

public class CameraActivity extends AppCompatActivity {

    public static final String RETURNED_IMAGE_ROTATION = "returned image rotation";
    public static final String DEFAULT_ACTION = "ms.camera.ACTION_IMAGE_CAPTURE";
    public static final String EXTRA_OUTPUT = "extra output";

    private Camera mCamera;
    private CameraView mCameraView = null;
    private Camera.PictureCallback jpegCallback;
    private int camBackId = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int camFrontId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private int mCurrentCameraId = camBackId;

    private int[] flashStatusIcons = {
            R.drawable.ic_flash_auto,
            R.drawable.ic_flash_on,
            R.drawable.ic_flash_off
    };

    private int mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;

    private String mCurrentFlashStatus = Camera.Parameters.FLASH_MODE_AUTO;

    private Bitmap photoTakenBitmap;

    private MediaPlayer mediaPlayer;

    private Uri photoTakenUri = null;

    private byte[] photoTakenByteData = null;

    ProgressBar progressBar;

    LinearLayout submitLayer;

    Button submitPhotoBt;

    Button cancelPhotoBt;

    ImageView takenPhotoView;

    ImageView takePhotoBt;

    ImageView changeFlashStatusBt;

    ImageView switchCameraFaceBt;

    public void switchCameraFaceClicked(){
        //TODO change camera face
        if(mCameraView==null)
            return;
        if (mCurrentCameraId == camBackId){
            mCurrentCameraId = camFrontId;
            toggleFlashButtonEnable(false);
        }else{
            mCurrentCameraId = camBackId;
            toggleFlashButtonEnable(true);
        }
        mCameraView.switchCameraFacing(mCurrentCameraId);

    }

    private void toggleFlashButtonEnable(boolean enable) {
        changeFlashStatusBt.setEnabled(enable);
        changeFlashStatusBt.setClickable(enable);
        int resId;
        switch (mCurrentFlashStatus){
            case Camera.Parameters.FLASH_MODE_ON:
                resId = enable ? R.drawable.ic_flash_on : R.drawable.ic_flash_on_disabled;
                break;
            case Camera.Parameters.FLASH_MODE_OFF:
                resId = enable ? R.drawable.ic_flash_off : R.drawable.ic_flash_off_disabled;
                break;
            case Camera.Parameters.FLASH_MODE_AUTO:
                resId = enable ? R.drawable.ic_flash_auto : R.drawable.ic_auto_flash_disabled;
                break;
            default:
                resId = enable ? R.drawable.ic_flash_auto : R.drawable.ic_auto_flash_disabled;
                break;
        }
        changeFlashStatusBt.setImageResource(resId);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
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
        //TODO save and set result for activity
        if(photoTakenByteData!=null && photoTakenUri!=null && photoTakenUri.getPath()!=null){
            File file = new File(photoTakenUri.getPath());
            try{
                FileOutputStream outputStream = new FileOutputStream(file);
                outputStream.write(photoTakenByteData);
                outputStream.flush();
                outputStream.close();
                Intent output = new Intent();
                output.putExtra(RETURNED_IMAGE_ROTATION,mCurrentCameraId==camBackId ? 90 : 270);
                output.setData(Uri.fromFile(file));
                setResult(RESULT_OK,output);
                finish();
            }catch (IOException e){
                e.printStackTrace();
            }


        }
    }
    public void cancelPhotoClicked(){
        //TODO cancel and hide submit layer
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
        if(mCameraView==null)
            return;
        if(!hasPermissions()) {
            requestPermissions(false);
        }else {
            mCameraView.takePicture(jpegCallback);
        }
    }



    public void changeFlashStatusClicked(){

        if(mCameraView==null)
            return;
        if(mCurrentFlashStatusIcon==R.drawable.ic_flash_auto){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_on;
            mCurrentFlashStatus = Camera.Parameters.FLASH_MODE_ON;
        }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_on){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_off;
            mCurrentFlashStatus = Camera.Parameters.FLASH_MODE_OFF;
        }else if(mCurrentFlashStatusIcon==R.drawable.ic_flash_off){
            mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;
            mCurrentFlashStatus = Camera.Parameters.FLASH_MODE_AUTO;
        }else {
            mCurrentFlashStatusIcon = R.drawable.ic_flash_auto;
            mCurrentFlashStatus = Camera.Parameters.FLASH_MODE_AUTO;
        }
        changeFlashStatusBt.setImageResource(mCurrentFlashStatusIcon);
        mCameraView.setFlashStatus(mCurrentFlashStatus);

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
                if(CameraActivity.this.isFinishing())
                    return;
                if(hasPermissions()){
                    if(!isCreated){
                        if (mCameraView != null) {
                            mCameraView.onResumeView();
                        }
                    }else{
                        initCamera();
                        getIntentDate(getIntent());
                    }
                }else {
                    handler.postDelayed(this,2000);
                }
            }
        },2000);
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
            if (mCameraView != null) {
                mCameraView.onResumeView();
            }
        }
    }

    private void initCamera() {

        try{
            mCamera = Camera.open();//you can use open(int) to use different cameras
        } catch (Exception e){
            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
        }

        if(mCamera != null) {
            mCameraView = new CameraView(this, mCamera);//create a SurfaceView to show camera data
            FrameLayout camera_view = (FrameLayout)findViewById(R.id.camera_view);
            camera_view.addView(mCameraView);//add the SurfaceView to the layout
        }

        if(jpegCallback==null){
            jpegCallback = new Camera.PictureCallback() {
                public void onPictureTaken(byte[] data, Camera camera) {
                    try {
                        camera.stopPreview();

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    photoTakenByteData = data;
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    photoTakenBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
                    if(photoTakenBitmap!=null) {
                        if(mCurrentCameraId==camBackId) {
                            photoTakenBitmap = rotate(photoTakenBitmap, 90);
                        }else {
                            photoTakenBitmap = rotate(photoTakenBitmap, 270);
                        }
                        takenPhotoView.setImageBitmap(photoTakenBitmap);
                        takenPhotoView.setVisibility(View.VISIBLE);
                        submitLayer.setVisibility(View.VISIBLE);
                        changeFlashStatusBt.setVisibility(View.GONE);
                        switchCameraFaceBt.setVisibility(View.GONE);
                        takePhotoBt.setVisibility(View.GONE);
                    }
                    if(mCameraView!=null){
                        mCameraView.refresh();
                    }
                }
            };
        }
    }

    private Uri getImageUri(Bitmap inImage, Context context) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Document", null);
        if(!inImage.isRecycled())inImage.recycle();
        return Uri.parse(path);
    }

    private Bitmap rotate(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            if(!bm.isRecycled())bm.recycle();
            return bmOut;
        }
        return bm;
    }

    public static Bitmap rotateBitmap(Bitmap bm, int rotation) {
        if (rotation != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            Bitmap bmOut = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
            if(!bm.isRecycled())bm.recycle();
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

    public class CameraView extends SurfaceView implements SurfaceHolder.Callback,cameraListener{

        private SurfaceHolder mHolder;
        private Camera mCamera;



        public CameraView(Context context, Camera camera){
            super(context);
            inits(camera);
        }

        private void inits(Camera camera) {
            mCamera = camera;
            mCamera.setDisplayOrientation(90);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            Camera.Size mSize = null;
            List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
            if(sizes!=null && !sizes.isEmpty()){
                int maxWidth = 0;
                for (Camera.Size size : sizes){
                    if(size.width>maxWidth){
                        maxWidth = size.width;
                        mSize = size;
                    }
                }
            }
            if(mSize!=null){
                parameters.setPictureSize(mSize.width,mSize.height);
            }
            mCamera.setParameters(parameters);
            //get the holder and set this class as the callback, so we can get camera data here
            mHolder = getHolder();
            mHolder.addCallback(this);
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        }

        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            try{
                //when the surface is created, we can set the camera to draw images in this surfaceholder
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("ERROR", "Camera error on surfaceCreated " + e.getMessage());
            }

        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
            refreshCamera();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            //our app has only one screen, so we'll destroy the camera in the surface
            //if you are unsing with more screens, please move this code your activity
            mCamera.stopPreview();
            mCamera.release();
        }

        public void refreshCamera() {
            //before changing the application orientation, you need to stop the preview, rotate and then start it again
            if(mHolder.getSurface() == null)//check if the surface is ready to receive camera data
                return;

            try{
                mCamera.stopPreview();
            } catch (Exception e){
                //this will happen when you are trying the camera if it's not running
            }

            //now, recreate the camera preview
            try{
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (IOException e) {
                Log.d("ERROR", "Camera error on surfaceChanged " + e.getMessage());
            }
        }

        @Override
        synchronized public void takePicture(final Camera.PictureCallback jpegCallback) {
            if(mCamera!=null) {
                //take the picture
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, final Camera camera) {
                        if(success){
                            camera.takePicture(new Camera.ShutterCallback() {
                                @Override
                                public void onShutter() {
                                    if(mediaPlayer!=null){
                                        mediaPlayer.start();
                                    }
                                }
                            },null, jpegCallback);
                        }else {
                            takePicture(jpegCallback);
                        }
                    }
                });


            }
        }

        @Override
        public void onResumeView() {
            try{
                mCamera = Camera.open();//you can use open(int) to use different cameras
            } catch (Exception e){
                Log.d("ERROR", "Failed to get camera: " + e.getMessage());
            }
            if(mCamera!=null){
                inits(mCamera);
            }
        }

        @Override
        public void refresh() {
            refreshCamera();
        }

        @Override
        public void switchCameraFacing(int cameraId) {
            int camNum = Camera.getNumberOfCameras();
            if (camNum > 1) {

                //if camera is running
                if (mCamera != null) {
                    //and there is more than one camera
                    if (camNum > 1) {
                        mCamera.stopPreview();
                        mCamera.release();
                        try{
                            mCamera= Camera.open(cameraId);
                            mCamera.setPreviewDisplay(getHolder());
                            mCamera.setDisplayOrientation(90);
                            mCamera.startPreview();

                        } catch (Exception e){
                            Log.d("ERROR", "Failed to get camera: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        @Override
        public void setFlashStatus(String flashStatus) {

            if (mCurrentCameraId == camFrontId)
                return;

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setFlashMode(flashStatus);
            mCamera.setParameters(parameters);

                /*if (mCamera != null) {
                    mCamera.stopPreview();
                    mCamera.release();
                    try{
                        mCamera=Camera.open(mCurrentCameraId);
                        Camera.Parameters parameters = mCamera.getParameters();
                        parameters.setFlashMode(flashStatus);
                        mCamera.setParameters(parameters);
                        mCamera.setPreviewDisplay(getHolder());
                        mCamera.setDisplayOrientation(90);
                        mCamera.startPreview();

                    } catch (Exception e){
                        Log.d("ERROR", "Failed to get camera: " + e.getMessage());
                        e.printStackTrace();
                    }
                }*/
            }
        }


    public interface cameraListener{
        void takePicture(Camera.PictureCallback jpegCallback);
        void onResumeView();
        void refresh();
        void switchCameraFacing(int cameraId);
        void setFlashStatus(String flashStatus);
    }
}
