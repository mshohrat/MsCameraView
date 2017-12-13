package com.github.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileNotFoundException;

import com.github.camera.CameraActivity;

public class MainActivity extends AppCompatActivity {

    Button takePhotoButton;
    ImageView photoView;
    int REQUEST_CODE = 1001;
    private static final String TEMP_IMAGE_NAME = "tempImage";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        takePhotoButton = (Button)findViewById(R.id.get_photo);
        photoView = (ImageView)findViewById(R.id.photo_view);

        takePhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File storageDir = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES);
                File file = new File(storageDir,TEMP_IMAGE_NAME);
                Intent intent = new Intent(CameraActivity.DEFAULT_ACTION);
                intent.putExtra(CameraActivity.EXTRA_OUTPUT, Uri.fromFile(file).toString());
                startActivityForResult(intent, REQUEST_CODE);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            Uri selectedImage = null;
            int rotation = 0;
            if(data!=null) {
                rotation = data.getIntExtra(CameraActivity.RETURNED_IMAGE_ROTATION, rotation);
                selectedImage = data.getData();
            }

            if(selectedImage!=null) {
                photoView.setImageURI(selectedImage);

            }
        }
    }
}
