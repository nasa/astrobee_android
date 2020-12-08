/* Copyright (c) 2017, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 *
 * All rights reserved.
 *
 * The Astrobee platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package gov.nasa.arc.irg.astrobee.sci_cam_image;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Switch;

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
public class SciCamImage extends Service {
    
    public boolean inUse;
    public boolean savePicturesToDisk;
    public boolean cameraBehaviorChanged; // when focus/exposure/etc changes happen
    public boolean doRun;
    public boolean continuousPictureTaking;
    public boolean takeSinglePicture;
    public static boolean doLog;

    public float focusDistance;
    public String focusMode;
    public String imageType;
    public int previewImageWidth; // Image width to use to publish previews over ROS
    public String dataPath;       // Where to store the acquired images on HLP
    
    private WindowManager windowManager;
    private NodeMainExecutor nodeMainExecutor;
        
    public SciCamPublisher sciCamPublisher;
    public CameraController cameraController;

    // When the last pic was acquired, in milliseconds since epoch.
    // An integer would be too short here.
    public long lastPicTime; 

    // the minimum spacing between pics in milliseconds
    public static final long minPicSpacing = 150;
        
    public static final String SCI_CAM_TAG = "sci_cam";

    // Constants to send commands to this app
    public static final String TAKE_SINGLE_PICTURE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TAKE_SINGLE_PICTURE";
    public static final String TAKE_SINGLE_PICTURE_WORKER
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TAKE_SINGLE_PICTURE_WORKER";
    public static final String TURN_ON_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_OFF_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_ON_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_SAVING_PICTURES_TO_DISK";
    public static final String TURN_OFF_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_SAVING_PICTURES_TO_DISK";
    public static final String SET_PREVIEW_IMAGE_WIDTH
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.SET_PREVIEW_IMAGE_WIDTH";
    public static final String SET_FOCUS_DISTANCE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.SET_FOCUS_DISTANCE";
    public static final String SET_FOCUS_MODE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.SET_FOCUS_MODE";
    public static final String SET_IMAGE_TYPE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.SET_IMAGE_TYPE";
    public static final String STOP
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.STOP";
    public static final String TURN_ON_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_LOGGING";
    public static final String TURN_OFF_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_LOGGING";

    @Override
    public void onCreate() {

        // This appears to be a better place for initializing
        // variables, just like doing everything else.
        doRun = true;
        continuousPictureTaking = false;
        takeSinglePicture = false;
        inUse = false;
        savePicturesToDisk = true;
        doLog = false;
        focusDistance = 0.39f;
        focusMode = "manual";
        imageType = "color";
        previewImageWidth = 640; // 0 will mean full-resolution
        cameraBehaviorChanged = true;  
        lastPicTime = -1;
        
        // Register intents
        registerReceiver(takeSinglePictureCmdReceiver,
                         new IntentFilter(TAKE_SINGLE_PICTURE));
        registerReceiver(takeSinglePictureWorkerCmdReceiver,
                         new IntentFilter(TAKE_SINGLE_PICTURE_WORKER));
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
        registerReceiver(setImageTypeCmdReceiver,
                         new IntentFilter(SET_IMAGE_TYPE));
        registerReceiver(setPreviewImageWidthCmdReceiver,
                         new IntentFilter(SET_PREVIEW_IMAGE_WIDTH));
        registerReceiver(stopCmdReceiver,
                         new IntentFilter(STOP));
        //getPermissions();

        // Start a foreground service to avoid an unexpected kill
        // and to be able to use the camera
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            Notification notification = new Notification.Builder(this)
                .setContentTitle("SciCamImage is taking pictures")
                .setContentText("").setSmallIcon(R.mipmap.ic_launcher).build();
            startForeground(1234, notification);
        }
        
        // Make the window size to 1x1, move it to the top left corner
        // and set this service as a callback. It will be barely
        // visible.
        windowManager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams
            (1, 1,
             WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
             WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
             PixelFormat.TRANSLUCENT
             //PixelFormat.OPAQUE
             );
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;
        
        AutoFitTextureView textureView = new AutoFitTextureView(this);
        windowManager.addView(textureView, layoutParams);
        
        // Start the camera controller 
        cameraController = new CameraController(this, textureView);
        
        startROS();
        
        startBackgroundThread();
        
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "finished onCreate");
    }

    public WindowManager getWindowManager() {
        return windowManager;
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
    
    //@Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startBackgroundThread(){

        // Create a thread which will periodically try tell the main
        // thread to take a picture. Android insists that only the
        // main thread can take picture, that is why we send it an
        // intent here, rather than taking the picture directly.
        Runnable r = new Runnable() {
                public void run() {
                    
                    while (SciCamImage.this.doRun) {

                        // If nothing needs to be done, or the camera is in use,
                        // or we barely acquired a picture, take a little break
                        boolean doWork = (SciCamImage.this.takeSinglePicture ||
                                          SciCamImage.this.continuousPictureTaking);
                        long currTime    = new Date().getTime();
                        long elapsedTime = currTime - SciCamImage.this.lastPicTime;
                        if (!doWork || 
                            SciCamImage.this.inUse ||
                            elapsedTime < SciCamImage.this.minPicSpacing) {
                            
                            try {
                                Thread.sleep(SciCamImage.minPicSpacing/8); // sleep (milliseconds)
                            }catch(InterruptedException e){
                            }

                            continue;
                        }

                        // If we'll take a single pic only, declare it taken
                        if (SciCamImage.this.takeSinglePicture) {
                            synchronized(SciCamImage.this) {
                                SciCamImage.this.takeSinglePicture = false;
                            }
                        }

                        // Declare the camera in use
                        synchronized(SciCamImage.this) {
                            inUse = true;
                        }

                        // Signal to the main thread to take the pic
                        Intent intent = new Intent();
                        intent.setAction(SciCamImage.TAKE_SINGLE_PICTURE_WORKER);
                        sendBroadcast(intent);
                    }
                }
            };
        Thread t = new Thread(r);
        t.start();
        
        // to make it stop
        // stopSelf();
    }
        
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Get the data path from Guest Science. If not specified
        // then use the default internal storage directory.
        dataPath = "";
        Bundle b = intent.getExtras();
        if (b != null && !b.get("data_path").equals("") ) {
            dataPath = b.get("data_path") + File.separator + "delayed";
        } else{
            dataPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                +  File.separator + "sci_cam_image";
        }
        Log.i(SCI_CAM_TAG, "Using data path " + dataPath);

        super.onStartCommand(intent, flags, startId);

        // Don't allow it to restart if stopped
        return START_NOT_STICKY;
    }
    
//     private void getPermissions(){
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//             if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
//                 //Requesting permission.
//                 ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
//             }
//         }
//     }
    
//     @Override //Override from ActivityCompat.OnRequestPermissionsResultCallback Interface
//     public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                                                int[] grantResults) {
//         switch (requestCode) {
//             case 1: {
//                 // If request is cancelled, the result arrays are empty.
//                 if (grantResults.length > 0 &&
//                     grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                     // permission granted
//                 }
//                 return;
//             }
//         }
//     }

    // Take a single picture via the background thread
    private BroadcastReceiver takeSinglePictureCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.takeSinglePictureFun();
            }
        };
    private void takeSinglePictureFun() {
        Log.i(SciCamImage.SCI_CAM_TAG, "Take a single picture");
        takeSinglePicture = true;
        continuousPictureTaking = false;
    }

    // Take a single picture. This method should not be used directly.
    private BroadcastReceiver takeSinglePictureWorkerCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.takeSinglePictureWorkerFun();
            }
        };
    private void takeSinglePictureWorkerFun() {
        cameraController.takePicture();
    }
        
    // A signal to turn on continuous picture taking
    private BroadcastReceiver turnOnContinuousPictureTakingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOnContinuousPictureTaking();
            }
        };
    private void turnOnContinuousPictureTaking() {
        Log.i(SciCamImage.SCI_CAM_TAG, "Turn on continuous picture taking");
        synchronized(this) {
            takeSinglePicture       = false;
            continuousPictureTaking = true;
        }
    }
    
    // A signal to turn off continuous picture taking
    private BroadcastReceiver turnOffContinuousPictureTakingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOffContinuousPictureTaking();
            }
        };
    private void turnOffContinuousPictureTaking() {
        Log.i(SciCamImage.SCI_CAM_TAG, "Turn off continuous picture taking");
        synchronized(this) {
            takeSinglePicture       = false;
            continuousPictureTaking = false;
        }
    }

    // A signal to turn on saving pictures to disk
    private BroadcastReceiver turnOnSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOnSavingPcituresToDisk();
            }
        };
    private void turnOnSavingPcituresToDisk() {
        Log.i(SCI_CAM_TAG, "Turn on saving pictures to disk");
        synchronized(this) {
            savePicturesToDisk = true;
        }
    }

    // A signal to turn off saving pictures to disk
    private BroadcastReceiver turnOffSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOffSavingPcituresToDisk();
            }
        };
    private void turnOffSavingPcituresToDisk() {
        Log.i(SCI_CAM_TAG, "Turn off saving pictures to disk");
        synchronized(this) {
            savePicturesToDisk = false;
        }
    }
    
    // A signal to turn on logging
    private BroadcastReceiver turnOnLoggingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOnLogging();
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
                SciCamImage.this.turnOffLogging();
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
                        SciCamImage.this.setFocusDistance(focus_distance);
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
        cameraBehaviorChanged = true;  
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
                    
                    SciCamImage.this.setFocusMode(focus_mode);
                } catch (Exception e) {
                    Log.e(SCI_CAM_TAG, "Failed to set the focus mode.");
                }    
            }
        };
    private void setFocusMode(String focus_mode) {
        focusMode = focus_mode;
        cameraBehaviorChanged = true;  
        Log.i(SCI_CAM_TAG, "Setting the focus mode: " + focusMode);
    }

    // A signal to set the image type to color or grayscale
    private BroadcastReceiver setImageTypeCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {

                    String image_type = intent.getStringExtra("image_type");
                    if (!image_type.equals("color") &&
                        !image_type.equals("grayscale")) {
                        Log.e(SCI_CAM_TAG, "Image type must be color or grayscale. " +
                              "Got: '" + image_type + "'");
                        return;
                    }
                    
                    SciCamImage.this.setImageType(image_type);
                } catch (Exception e) {
                    Log.e(SCI_CAM_TAG, "Failed to set the image type.");
                }    
            }
        };
    private void setImageType(String image_type) {
        imageType = image_type;
        Log.i(SCI_CAM_TAG, "Setting the image type: " + imageType);
    }

    // A signal to set the preview image width (for ROS)
    private BroadcastReceiver setPreviewImageWidthCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {
                    int preview_image_width = 0;
                    String str = intent.getStringExtra("preview_image_width");
                    preview_image_width = Integer.parseInt(str);
                    if (preview_image_width >= 0) {
                        SciCamImage.this.setPreviewImageWidth(preview_image_width);
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

    // A signal to quit the app
    private BroadcastReceiver stopCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // The lines below will call onDestroy(), which will call quitThisApp()
                SciCamImage.this.stopForeground(true);
                SciCamImage.this.stopSelf();
            }
        };

    @Override
    public void onDestroy() {
        SciCamImage.this.quitThisApp();
        super.onDestroy();
    }

    private void quitThisApp() {
        Log.i(SCI_CAM_TAG, "Quitting SciCamImage");
        doRun = false; // This will make the background thread stop

        if(cameraController != null) {
            cameraController.closeCamera();
            cameraController = null;
        }
        //System.exit(0); // may be unnecessary
    }

}
