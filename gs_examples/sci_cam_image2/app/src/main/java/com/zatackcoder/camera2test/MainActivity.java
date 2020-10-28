package com.zatackcoder.camera2test;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    CameraControllerV2WithPreview ccv2WithPreview;
    CameraControllerV2WithoutPreview ccv2WithoutPreview;

    AutoFitTextureView textureView;
    Switch startstoppreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();

        boolean showpreview = intent.getBooleanExtra("showpreview", false);

        textureView = (AutoFitTextureView)findViewById(R.id.textureview);
        startstoppreview = (Switch) findViewById(R.id.startstoppreview);

        if(showpreview) {
            ccv2WithPreview = new CameraControllerV2WithPreview(MainActivity.this, textureView);
            startstoppreview.setChecked(true);
        } else {
            ccv2WithoutPreview = new CameraControllerV2WithoutPreview(getApplicationContext());
            startstoppreview.setChecked(false);
        }


        startstoppreview.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {

                if(startstoppreview.isChecked()) {
                    intent.putExtra("showpreview", true);
                    finish();
                    startActivity(intent);

                } else {
                    intent.putExtra("showpreview", false);
                    finish();
                    startActivity(intent);
                }
            }
        });

        findViewById(R.id.getpicture).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(startstoppreview.isChecked() && ccv2WithPreview != null) {
                    ccv2WithPreview.takePicture();
                } else if(ccv2WithoutPreview != null){
                    ccv2WithoutPreview.openCamera();
                    try { Thread.sleep(20); } catch (InterruptedException e) {}
                    ccv2WithoutPreview.takePicture();
                }

                Toast.makeText(getApplicationContext(), "Picture Clicked", Toast.LENGTH_SHORT).show();
            }
        });

        getPermissions();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        if(ccv2WithPreview != null) {
//            ccv2WithPreview.closeCamera();
//        }
//        if(ccv2WithoutPreview != null) {
//            ccv2WithoutPreview.closeCamera();
//        }
    }

    private void getPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                //Requesting permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }

    @Override //Override from ActivityCompat.OnRequestPermissionsResultCallback Interface
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                }
                return;
            }
        }
    }
}
