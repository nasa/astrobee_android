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
import android.os.Environment;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.io.File;

import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SciCamImage2 extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    AutoFitTextureView textureView;

    public volatile boolean inUse;
    public volatile boolean continuousPictureTaking;
    public volatile boolean takeSinglePicture;
    public volatile boolean savePicturesToDisk;
    public volatile boolean doQuit;
    public static boolean doLog;

    public float focusDistance;
    public String focusMode;
    public int previewImageWidth;
    public String dataPath;
    
    public CameraController cameraController;
    public SciCamPublisher sciCamPublisher;
    private Thread pictureThread;
    private NodeMainExecutor nodeMainExecutor;

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
    public static final String SET_FOCUS_DISTANCE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_FOCUS_DISTANCE";
    public static final String SET_FOCUS_MODE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_FOCUS_MODE";
    public static final String SET_PREVIEW_IMAGE_WIDTH
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_PREVIEW_IMAGE_WIDTH";
//     public static final String SET_DATA_PATH
//         = "gov.nasa.arc.irg.astrobee.sci_cam_image2.SET_DATA_PATH";
    public static final String STOP
        = "gov.nasa.arc.irg.astrobee.sci_cam_image2.STOP";

    public SciCamImage2() {
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(SciCamImage2.SCI_CAM_TAG, "Creating SciCamImage2.");

        super.onCreate(savedInstanceState);

        // This appears to be a better place for initializing
        // variables, just like doing everything else.
        
        inUse = false;
        continuousPictureTaking = false;
        takeSinglePicture = false;
        savePicturesToDisk = false;
        doQuit = false;
        doLog = false;
        focusDistance = 0.39f;
        focusMode = "manual";
        previewImageWidth = 0; // 0 will mean full-resolution
        dataPath = "";
        
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
        registerReceiver(setFocusDistanceCmdReceiver,
                         new IntentFilter(SET_FOCUS_DISTANCE));
        registerReceiver(setFocusModeCmdReceiver,
                         new IntentFilter(SET_FOCUS_MODE));
        registerReceiver(setPreviewImageWidthCmdReceiver,
                         new IntentFilter(SET_PREVIEW_IMAGE_WIDTH));
//         registerReceiver(setDataPathCmdReceiver,
//                          new IntentFilter(SET_DATA_PATH));
        registerReceiver(stopCmdReceiver,
                         new IntentFilter(STOP));
        
        // Set up the camera layout and the preview
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textureView = (AutoFitTextureView)findViewById(R.id.textureview);

        cameraController = new CameraController(SciCamImage2.this, textureView);
        
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
                    if (SciCamImage2.doLog)
                        Log.i(SCI_CAM_TAG, "Picture taken");
                    
                }
            });
        
        getPermissions();

        startROS();

        //A separate thread used to take pictures
        pictureThread = new Thread(new PictureThread(this)); 
        pictureThread.start();
         
        if (SciCamImage2.doLog)
            Log.i(SCI_CAM_TAG, "finished onCreate");

        Log.i(SCI_CAM_TAG, "Donezz");
    }
    
    void startROS() {
        try {
            if (doLog)
                Log.i(SCI_CAM_TAG, "Trying to start ROS");
            
            String uri_str = "http://llp:11311";
            URI masterURI = new URI(uri_str);
            
            Log.i(SCI_CAM_TAG, "Host is " + masterURI.getHost());
            Log.i(SCI_CAM_TAG, "Port is " + masterURI.getPort());

            NodeConfiguration nodeConfiguration = NodeConfiguration
                .newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration.setMasterUri(masterURI);
            
            nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

            if (doLog)
                Log.i(SCI_CAM_TAG, "Create SciCamPublisher");
            sciCamPublisher = new SciCamPublisher();
            
            nodeMainExecutor.execute(sciCamPublisher, nodeConfiguration);
            Log.i(SCI_CAM_TAG, "Started ROS");
            
        } catch (Exception e) {
            Log.i(SCI_CAM_TAG, "Failed to start ROS: " + e.getMessage());
            // Socket problem
        }
        
    }
    
//     @Override
//     protected void onStart() {
//         Log.i(SCI_CAM_TAG, "Starting SciCamImage2");
//         Intent in = getIntent();
//         Bundle b = in.getExtras();
//         if (b != null) {
//             String j = (String)b.get("data_path");
//             Log.i(SCI_CAM_TAG, "Got data path " + j);
//         } else{
//             Log.i(SCI_CAM_TAG, "Bundle is null ");
//         }
//     }        

    @Override
    protected void onStop() {
        Log.i(SCI_CAM_TAG, "Stopping4 SciCamImage2");
    }
    
    @Override
    protected void onDestroy() {
        Log.i(SCI_CAM_TAG, "Destroying SciCamImage2");
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
                SciCamImage2.this.takeSinglePictureFun();
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
                SciCamImage2.this.turnOnContinuousPictureTaking();
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
                SciCamImage2.this.turnOffContinuousPictureTaking();
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
                SciCamImage2.this.turnOnSavingPcituresToDisk();
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
                SciCamImage2.this.turnOffSavingPcituresToDisk();
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
                SciCamImage2.this.turnOnLogging();
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
                SciCamImage2.this.turnOffLogging();
            }
        };
    private void turnOffLogging() {
        Log.i(SCI_CAM_TAG, "Turn off logging");
        doLog = false;
    }

    // A signal to set the focus distance
    private BroadcastReceiver setFocusDistanceCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    float focus_distance = 0.0f;
                    String str = intent.getStringExtra("focus_distance");
                    focus_distance = Float.parseFloat(str);
                    if (focus_distance >= 0) {
                        SciCamImage2.this.setFocusDistance(focus_distance);
                    }else{
                        Log.e(SCI_CAM_TAG, "Focus distance must be non-negative.");
                    }
                } catch (Exception e) {
                    Log.e(SCI_CAM_TAG, "Failed to set the focus distance.");
                }    
            }
        };
    private void setFocusDistance(float focus_distance) {
        focusDistance = focus_distance;
        Log.i(SCI_CAM_TAG, "Setting the focus distance: " + focusDistance);
    }
    
    // A signal to set the focus mode to manual or auto
    private BroadcastReceiver setFocusModeCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {

                    String focus_mode = intent.getStringExtra("focus_mode");
                    if (!focus_mode.equals("manual") &&
                        !focus_mode.equals("auto")) {
                        Log.e(SCI_CAM_TAG, "Focus mode must be manual or auto. " +
                              "Got: '" + focus_mode + "'");
                        return;
                    }
                    
                    SciCamImage2.this.setFocusMode(focus_mode);
                } catch (Exception e) {
                    Log.e(SCI_CAM_TAG, "Failed to set the focus mode.");
                }    
            }
        };
    private void setFocusMode(String focus_mode) {
        focusMode = focus_mode;
        Log.i(SCI_CAM_TAG, "Setting the focus mode: " + focusMode);
    }

    // A signal to set the focus mode to manual or auto
    private BroadcastReceiver setPreviewImageWidthCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    int preview_image_width = 0;
                    String str = intent.getStringExtra("preview_image_width");
                    preview_image_width = Integer.parseInt(str);
                    if (preview_image_width >= 0) {
                        SciCamImage2.this.setPreviewImageWidth(preview_image_width);
                    }else{
                        Log.e(SCI_CAM_TAG, "Preview image width must be non-negative.");
                    }
                } catch (Exception e) {
                    Log.e(SCI_CAM_TAG, "Failed to set the preview image width.");
                }    
            }
        };
    private void setPreviewImageWidth(int preview_image_width) {
        previewImageWidth = preview_image_width;
        Log.i(SCI_CAM_TAG, "Setting the preview image width: " + previewImageWidth);
    }

//     // A signal to set the directory where the data should be written
//     private BroadcastReceiver setDataPathCmdReceiver = new BroadcastReceiver() {
//             @Override
//             public void onReceive(Context context, Intent intent) {
//                 try {
//                     String data_path = intent.getStringExtra("data_path");
//                     SciCamImage2.this.setDataPath(data_path);
//                 } catch (Exception e) {
//                     Log.e(SCI_CAM_TAG, "Failed to set the data path.");
//                 }    
//             }
//         };
//     private void setDataPath(String data_path) {
//         dataPath = data_path;
//         Log.i(SCI_CAM_TAG, "Setting the data path: " + dataPath);
//     }

    // A signal to quit the app
    private BroadcastReceiver stopCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage2.this.quitThisApp();
            }
        };
    private void quitThisApp() {
        Log.i(SCI_CAM_TAG, "Quitting SciCamImage2");
        doQuit = true; // This will make pictureThread stop.

        if(cameraController != null) {
            cameraController.closeCamera();
            cameraController = null;
        }

        finishAndRemoveTask();
        System.exit(0); // may be unnecessary
    }
    
}
