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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Runnable;
import java.lang.Thread;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

public class SciCamImage extends Activity  {
    private Context myContext;
    private LinearLayout cameraPreview;
    private Thread pictureThread;
    private int numPics;
    private SciCamPublisher sciCamPublisher;
    NodeMainExecutor nodeMainExecutor;
    
    // These will be used by the picture thread
    public Camera mCamera;
    public CameraPreview mPreview;
    public  PictureCallback mPicture;
    // Make these volatile so that the compiler does not optimize them away
    // (not sure about that, but it is safer that way).
    public volatile boolean inUse;
    public volatile boolean continuousPictureTaking;
    public volatile boolean takeSinglePicture;
    public volatile boolean savePicturesToDisk;
    public volatile boolean doQuit;
    public static boolean doLog;
    
    public static final String SCI_CAM_TAG = "sci_cam";

    // Constants to send commands to this app
    public static final String TAKE_SINGLE_PICTURE
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TAKE_SINGLE_PICTURE";
    public static final String TURN_ON_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_OFF_CONTINUOUS_PICTURE_TAKING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_CONTINUOUS_PICTURE_TAKING";
    public static final String TURN_ON_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_LOGGING";
    public static final String TURN_OFF_LOGGING
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_LOGGING";
    public static final String TURN_ON_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_ON_SAVING_PICTURES_TO_DISK";
    public static final String TURN_OFF_SAVING_PICTURES_TO_DISK
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.TURN_OFF_SAVING_PICTURES_TO_DISK";
    public static final String STOP
        = "gov.nasa.arc.irg.astrobee.sci_cam_image.STOP";

    public SciCamImage() {
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // This appears to be a better place for initializing
        // variables, just like doing everything else.
        
        inUse = false;
        continuousPictureTaking = false;
        takeSinglePicture = false;
        savePicturesToDisk = false;
        doQuit = false;
        doLog = false;
        numPics = 0;

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
        
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        myContext = this;
        
        cameraPreview = (LinearLayout) findViewById(R.id.camera_preview);
        mPreview = new CameraPreview(myContext, mCamera);
        cameraPreview.addView(mPreview);
            
        startROS();

        // A separate thread used to take pictures
        pictureThread = new Thread(new PictureThread(this)); 
        pictureThread.start();
    }

    void startROS() {

        // TODO(oalexan1): Use the enviromental variable! And check if it is set!
        
        try {
            Log.i(SCI_CAM_TAG, "Trying to start ROS");
            
            //String uri_str = "http://192.168.1.127:11311";
            String uri_str = "http://llp:11311";
            URI masterURI = new URI(uri_str);
            
            Log.i(SCI_CAM_TAG, "Host is " + masterURI.getHost());
            Log.i(SCI_CAM_TAG, "Port is " + masterURI.getPort());

            NodeConfiguration nodeConfiguration = NodeConfiguration
                .newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            nodeConfiguration.setMasterUri(masterURI);
            
            nodeMainExecutor = DefaultNodeMainExecutor.newDefault();

            if (SciCamImage.doLog)
                Log.i(SCI_CAM_TAG, "Create SciCamPublisher");
            sciCamPublisher = new SciCamPublisher();
            
            nodeMainExecutor.execute(sciCamPublisher, nodeConfiguration);
            Log.i(SCI_CAM_TAG, "Started ROS");
            
        } catch (Exception e) {
            Log.i(SCI_CAM_TAG, "Failed to start ROS: " + e.getMessage());
            // Socket problem
        }
        
    }
    
    private int findBackFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "Number of cameras: " + numberOfCameras);

        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                if (SciCamImage.doLog)
                    Log.i(SCI_CAM_TAG, "Found front facing camera");
            }
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                if (SciCamImage.doLog)
                    Log.i(SCI_CAM_TAG, "Found back facing camera");
            }
        }

        for (int i = 0; i < numberOfCameras; i++) {
            CameraInfo info = new CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public void onResume() {
        super.onResume();
        
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "onResume started");

        if (!hasCamera(myContext)) {
            Toast toast = Toast.makeText(myContext,
                                         "Sorry, your phone does not have a camera!",
                                         Toast.LENGTH_LONG);
            toast.show();
            finish();
        }
        
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "Release camera");
        releaseCamera();
        
        if (findBackFacingCamera() < 0) {
            // Log.e(SCI_CAM_TAG, "No back-facing camera found");
        }			
        mCamera = Camera.open(findBackFacingCamera());
        setupCamera();
        mPicture = getPictureCallback();
        mPreview.refreshCamera(mCamera);
        
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "onResume finished");
    }

  private void setupCamera() {
      if (SciCamImage.doLog)
          Log.i(SCI_CAM_TAG, "Setting up the camera");

      Camera.Parameters camParams = mCamera.getParameters();
      camParams.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);

      // TODO(oalexan1): Play with this.
      // int exp = camParams.getExposureCompensation();
      // int max_exp = camParams.getMaxExposureCompensation();
      // int min_exp = camParams.getMinExposureCompensation();
      // camParams.set("exposure-time", "0.001");
      // TODO(oalexan1): This has very useful info!
      // Log.i(SCI_CAM_TAG, " exposure flatten " + camParams.flatten());
      // Log.i(SCI_CAM_TAG, " exposure  " + String.valueOf(exp) + " with range " + min_exp + " " + max_exp);
      
      mCamera.setParameters(camParams);
  }

    private boolean hasCamera(Context context) {
        // Check if the device has camera
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }
    
    private PictureCallback getPictureCallback() {
        PictureCallback picture = new PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    if (SciCamImage.doLog)
                        Log.i(SCI_CAM_TAG, "Picture taken, will process it");
                    Camera.Parameters camParams = mCamera.getParameters();
                    Camera.Size       s         = camParams.getPictureSize();
                    sciCamPublisher.onNewRawImage(data, s);
                    if (SciCamImage.doLog)
                        Log.i(SCI_CAM_TAG, "Picture processed");

                    if (savePicturesToDisk) {
                        // Debug code to save the file
                        try {
                            File pictureFile = getOutputMediaFile();
                            if (pictureFile == null) {
                                Log.i(SCI_CAM_TAG, "Failed to create a picture file");
                                return;
                            }
                            FileOutputStream fos = new FileOutputStream(pictureFile);
                            fos.write(data);
                            fos.close();
                            Toast toast = Toast.makeText(myContext, "Picture saved: " + pictureFile,
                                                         Toast.LENGTH_LONG);
                            toast.show();
                            Log.i(SCI_CAM_TAG, "Saved picture to: " + pictureFile);
                        } catch (IOException e) {
                            Log.i(SCI_CAM_TAG, "Failed to save picture to disk.");
                        }
                    }
                    
                    // Protect variables used in the other thread
                    synchronized(this){
                        numPics = numPics + 1;
                        //mPreview.refreshCamera(mCamera);
                        inUse = false; // done processing the picture
                    }
                }
            };
        return picture;
    }

    private static String getPictureDir() {
        File sdDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return sdDir.toString();
    }
    
    // Create the name of a file in which to save the picture
    private static File getOutputMediaFile() {
        //make a new file directory inside the "sdcard" folder
        File mediaStorageDir = new File(getPictureDir());
            
        if (!mediaStorageDir.exists()) {
            //if you cannot make this folder return
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
            
        //take the current timestamp
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        //and make a media file
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                                  + "IMG_" + timeStamp + ".jpg");

        return mediaFile;
    }
    
    private void releaseCamera() {
        // stop and release camera
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "release camera!");
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    // When the user minimizes the app just quit
    @Override
    protected void onPause() {
        super.onPause();
        quitThisApp();
    }

    // A signal to enter single picture mode, and take a picture. The actual work happens
    // in PictureThread. 
    private BroadcastReceiver takeSinglePictureCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.takeSinglePictureFun();
            }
        };
    private void takeSinglePictureFun() {
        Log.i(SCI_CAM_TAG, "Take a single picture");
        synchronized(this){
            continuousPictureTaking = false;
            takeSinglePicture       = true;
        }
    }
    
    // A signal to turn on continuous picture taking
    private BroadcastReceiver turnOnContinuousPictureTakingCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOnContinuousPictureTaking();
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
                SciCamImage.this.turnOffContinuousPictureTaking();
            }
        };
    private void turnOffContinuousPictureTaking() {
        Log.i(SCI_CAM_TAG, "Turn off continuous picture taking");
        synchronized(this){
            takeSinglePicture       = false;
            continuousPictureTaking = false;
        }
    }

    // A signal to turn on continuous picture taking
    private BroadcastReceiver turnOnSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOnSavingPcituresToDisk();
            }
        };
    private void turnOnSavingPcituresToDisk() {
        Log.i(SCI_CAM_TAG, "Turn on saving pictures to disk");
        synchronized(this){
            savePicturesToDisk = true;
        }
    }

    // A signal to turn off continuous picture taking
    private BroadcastReceiver turnOffSavingPcituresToDiskCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.turnOffSavingPcituresToDisk();
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

    // A signal to quit the app
    private BroadcastReceiver stopCmdReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SciCamImage.this.quitThisApp();
            }
        };
    private void quitThisApp() {
        if (SciCamImage.doLog)
            Log.i(SCI_CAM_TAG, "Release the camera and quit");
        doQuit = true; // This will make pictureThread stop.
        releaseCamera();
        finishAndRemoveTask();
        System.exit(0); // may be unnecessary
    }
    
}