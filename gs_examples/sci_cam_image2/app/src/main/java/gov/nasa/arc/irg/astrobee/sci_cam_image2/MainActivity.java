package gov.nasa.arc.irg.astrobee.sci_cam_image2;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;
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
import android.util.Log;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    CameraController cc;
    AutoFitTextureView textureView;
    Switch startstoppreview;

    public static final String SCI_CAM_TAG = "sci_cam";

    public static final String TAKE_SINGLE_PICTURE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TAKE_SINGLE_PICTURE";

    public MainActivity() {
        Log.i(SCI_CAM_TAG, "Main started");
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);

        Log.i(SCI_CAM_TAG, "start onCreate");
        
        Log.i(SCI_CAM_TAG, "register receiver");
        registerReceiver(takeSinglePictureCmdReceiver,
                         new IntentFilter(TAKE_SINGLE_PICTURE));

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Intent intent = getIntent();

        boolean showpreview = intent.getBooleanExtra("showpreview", true);

        textureView = (AutoFitTextureView)findViewById(R.id.textureview);
        startstoppreview = (Switch) findViewById(R.id.startstoppreview);

        Log.i(SCI_CAM_TAG, "test for preview");
        
        Log.i(SCI_CAM_TAG, "start camera with preview");
        cc = new CameraController(MainActivity.this, textureView);
        startstoppreview.setChecked(true);

        startstoppreview.setOnClickListener(new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            
            if (startstoppreview.isChecked()) {
                Log.i(SCI_CAM_TAG, "Trying to start preview activity");
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
           if(startstoppreview.isChecked() && cc != null) {
               Log.i(SCI_CAM_TAG, "Trying to take picture with preview");
               cc.takePicture();
           }
           
           Toast.makeText(getApplicationContext(), "Picture taken", Toast.LENGTH_SHORT).show();
           Log.i(SCI_CAM_TAG, "Picture taken");
           
       }
       });
       
       getPermissions();
       
       Log.i(SCI_CAM_TAG, "finished onCreate");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // if(cc != null) {
        //   cc.closeCamera();
        // }
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
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                               int[] grantResults) {
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

    // A signal to enter single picture mode, and take a picture. The actual work happens
    // in PictureThread. 
    private BroadcastReceiver takeSinglePictureCmdReceiver = new BroadcastReceiver() {
            @Override
                public void onReceive(Context context, Intent intent) {
                MainActivity.this.takeSinglePictureFun();
            }
        };
    private void takeSinglePictureFun() {
        
        if(cc != null){
            Log.i(SCI_CAM_TAG, "2Trying to take a picture with preview");
            cc.takePicture();
        }
        
        synchronized(this){
            //  continuousPictureTaking = false;
            // takeSinglePicture       = true;
        }
    }
    
}
