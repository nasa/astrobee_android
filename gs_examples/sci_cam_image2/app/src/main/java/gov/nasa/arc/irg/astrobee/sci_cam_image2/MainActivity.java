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

    AutoFitTextureView textureView;

    // Make these volatile so that the compiler does not optimize them away
    // (not sure about that, but it is safer that way).
    public volatile boolean inUse;
    public volatile boolean continuousPictureTaking;
    public volatile boolean takeSinglePicture;
    public volatile boolean savePicturesToDisk;
    public volatile boolean doQuit;
    public static boolean doLog;
    
    public CameraController cameraController;
    private Thread pictureThread;

    public static final String SCI_CAM_TAG = "sci_cam";

    // Constants to send commands to this app
    public static final String TAKE_SINGLE_PICTURE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TAKE_SINGLE_PICTURE";
    public static final String TURN_ON_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_OFF_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_ON_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_LOGGING";
    public static final String TURN_OFF_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_LOGGING";
    public static final String TURN_ON_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_ON_SAVING_PICTURES_TO_DISK";
    public static final String TURN_OFF_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.TURN_OFF_SAVING_PICTURES_TO_DISK";
    public static final String STOP
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.STOP";

    public MainActivity() {
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);

        // This appears to be a better place for initializing
        // variables, just like doing everything else.
        
        inUse = false;
        continuousPictureTaking = false;
        takeSinglePicture = false;
        savePicturesToDisk = false;
        doQuit = false;
        doLog = false;

        // Register intents
        registerReceiver(takeSinglePictureCmdReceiver,
                         new IntentFilter(TAKE_SINGLE_PICTURE));
        registerReceiver(turnOnContinuousPictureTakingCmdReceiver,
                         new IntentFilter(TURN_ON_CONTINUOUS_PICTURE_TAKING));
        registerReceiver(turnOffContinuousPictureTakingCmdReceiver,
                         new IntentFilter(TURN_OFF_CONTINUOUS_PICTURE_TAKING));
        registerReceiver(turnOnLoggingCmdReceiver,
                         new IntentFilter(TURN_ON_LOGGING));
        registerReceiver(turnOffLoggingCmdReceiver,
                         new IntentFilter(TURN_OFF_LOGGING));
        registerReceiver(turnOnSavingPcituresToDiskCmdReceiver,
                         new IntentFilter(TURN_ON_SAVING_PICTURES_TO_DISK));
        registerReceiver(turnOffSavingPcituresToDiskCmdReceiver,
                         new IntentFilter(TURN_OFF_SAVING_PICTURES_TO_DISK));
        registerReceiver(stopCmdReceiver,
                         new IntentFilter(STOP));
        
        // Set up the camera layout and the preview
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textureView = (AutoFitTextureView)findViewById(R.id.textureview);

        cameraController = new CameraController(MainActivity.this, textureView);
        
        // Allow the user to take a picture manually, by clicking 
        findViewById(R.id.getpicture).setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View view) {
                    if(cameraController != null) {
                        Log.i(SCI_CAM_TAG, "Trying to take picture with preview");
                        takeSinglePictureFun();
                    }
                    
                    Toast.makeText(getApplicationContext(), "Picture taken",
                                   Toast.LENGTH_SHORT).show();
                    Log.i(SCI_CAM_TAG, "Picture taken");
                    
                }
            });
        
        getPermissions();

        // A separate thread used to take pictures
        pictureThread = new Thread(new PictureThread(this)); 
        pictureThread.start();
        
        Log.i(SCI_CAM_TAG, "finished onCreate");
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraController != null) {
            cameraController.closeCamera();
            cameraController = null;
        }
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
                if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                }
                return;
            }
        }
    }

    // Allow the user to take a picture in debug mode with adb
    private BroadcastReceiver takeSinglePictureCmdReceiver = new BroadcastReceiver() {
            @Override
                public void onReceive(Context context, Intent intent) {
                MainActivity.this.takeSinglePictureFun();
            }
        };
    private void takeSinglePictureFun() {
        synchronized(this){
            // Turn on the flag to take a single picture. Then the
            // pictureThread will call the cameraController to take
            // it.
            continuousPictureTaking = false;
            takeSinglePicture       = true;
        }
    }

    // A signal to turn on continuous picture taking
    private BroadcastReceiver turnOnContinuousPictureTakingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOnContinuousPictureTaking();
            }
        };
    private void turnOnContinuousPictureTaking() {
        Log.i(SCI_CAM_TAG, "Turn on continuous picture taking");
        synchronized(this){
            takeSinglePicture       = false;
            continuousPictureTaking = true;
        }
    }

    // A signal to turn off continuous picture taking
    private BroadcastReceiver turnOffContinuousPictureTakingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOffContinuousPictureTaking();
            }
        };
    private void turnOffContinuousPictureTaking() {
        Log.i(SCI_CAM_TAG, "Turn off continuous picture taking");
        synchronized(this){
            takeSinglePicture       = false;
            continuousPictureTaking = false;
        }
    }

    // A signal to turn on saving pictures to disk
    private BroadcastReceiver turnOnSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOnSavingPcituresToDisk();
            }
        };
    private void turnOnSavingPcituresToDisk() {
        Log.i(SCI_CAM_TAG, "Turn on saving pictures to disk");
        synchronized(this){
            savePicturesToDisk = true;
        }
    }

    // A signal to turn off saving pictures to disk
    private BroadcastReceiver turnOffSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOffSavingPcituresToDisk();
            }
        };
    private void turnOffSavingPcituresToDisk() {
        Log.i(SCI_CAM_TAG, "Turn off saving pictures to disk");
        synchronized(this){
            savePicturesToDisk = false;
        }
    }
    
    // A signal to turn on logging
    private BroadcastReceiver turnOnLoggingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOnLogging();
            }
        };
    private void turnOnLogging() {
        Log.i(SCI_CAM_TAG, "Turn on logging");
        doLog = true;
    }

    // A signal to turn off logging
    private BroadcastReceiver turnOffLoggingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.turnOffLogging();
            }
        };
    private void turnOffLogging() {
        Log.i(SCI_CAM_TAG, "Turn off logging");
        doLog = false;
    }

    // A signal to quit the app
    private BroadcastReceiver stopCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                MainActivity.this.quitThisApp();
            }
        };
    private void quitThisApp() {
        if (MainActivity.doLog)
            Log.i(SCI_CAM_TAG, "Release the camera and quit");
        doQuit = true; // This will make pictureThread stop.

        if(cameraController != null) {
            cameraController.closeCamera();
            cameraController = null;
        }

        finishAndRemoveTask();
        System.exit(0); // may be unnecessary
    }
    
}
