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
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.TextView;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.ros.address.InetAddressFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class StartSciCamImage extends StartGuestScienceService {
    public static final String TAG = "SciCamImage";

    private CameraController mCameraController = null;

    private NodeConfiguration mNodeConfiguration = null;

    private NodeMainExecutor mNodeMainExecutor = null;

    private SciCamPublisher mSciCamPublisher = null;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        // Inform GDS/the ground that this APK received a command
        sendReceivedCustomCommand("info");

        String commandResult = "{\"Summary\": ";

        try {
            JSONObject obj = new JSONObject(command);
            String commandStr = obj.getString("name");

            switch (commandStr) {
                case "takePicture":
                    Log.d(TAG, "Received take picture command. Attempting to capture an image!");
                    // Make sure we were successful in opening the camera
                    if (mCameraController.isCameraOpen()) {
                        // Make sure the camera isn't already trying to capture an image and that we
                        // aren't continuously taking pictures
                        if (!mCameraController.getCameraInUse() &&
                                !mCameraController.getContinuousPictureTaking()) {
                            // Check to see if the haz distance is valid. If it is, we may need to
                            // set the focal distance. If it isn't, we can just take the picture.
                            if (obj.has("haz_dist")) {
                                float hazCamDistance = (float) obj.getDouble("haz_dist");
                                if (hazCamDistance > 0) {
                                    mCameraController.startManualFocusCapture(hazCamDistance);
                                    commandResult += "\"Started manual focus capture!\"}";
                                } else {
                                    mCameraController.captureImage();
                                    commandResult += "\"Capture request sent to the camera!\"}";
                                }
                            } else {
                                mCameraController.captureImage();
                                commandResult += "\"Capture request sent to the camera!\"}";
                            }
                        } else {
                            commandResult += "\"Error: Camera is in use!\"}";
                        }
                    } else {
                        commandResult += "\"Error: Camera is not open. Try restarting the apk!\"}";
                    }
                    break;
                case "setAutoExposure":
                    Log.d(TAG, "Received set auto exposure command.");
                    if (obj.has("auto")) {
                        boolean auto = obj.getBoolean("auto");
                        if (mCameraController.setAutoExposure(auto)) {
                            if (auto) {
                                commandResult += "Auto exposure turned on.\"}";
                            } else {
                                commandResult += "Auto exposure turned off.\"}";
                            }
                        } else {
                            commandResult += "Unable to set auto exposure.\"}";
                        }
                    } else {
                        commandResult += "Error: Auto argument not provided in the set auto exposure command.\"}";
                    }
                    break;
                case "setContinuousPictureTaking":
                    Log.d(TAG, "Received set continuous picture taking command.");
                    // Make sure we were successful in opening the camera
                    if (mCameraController.isCameraOpen()) {
                        if (obj.has("continuous")) {
                            boolean continuous = obj.getBoolean("continuous");
                            boolean currentlyContinuous = mCameraController.getContinuousPictureTaking();
                            mCameraController.setContinuousPictureTaking(continuous);
                            if (continuous && !currentlyContinuous) {
                                mCameraController.captureImage();
                                commandResult += "Started capturing images!\"}";
                            } else if (!continuous && currentlyContinuous) {
                                commandResult += "Stopped capturing images!\"}";
                            } else {
                                commandResult += "Already performing what was requested in the set continuous picture taking command.\"}";
                            }
                        } else {
                            commandResult += "Error: Continuous argument not provided in the set continuous picture taking command.\"}";
                        }
                    } else {
                        commandResult += "\"Error: Camera is not open. Try restarting the apk!\"}";
                    }
                    break;
                case "setFocusDistance":
                    Log.d(TAG, "Received set focus distance command.");
                    if (obj.has("distance")) {
                        float distance = (float) obj.getDouble("distance");
                        if (mCameraController.setFocusDistance(distance)) {
                            commandResult += "Focus distance set to " + distance + ".\"}";
                        } else {
                            commandResult += "Focus distance was either invalid or the camera failed to be configured.\"}";
                        }
                    } else {
                        commandResult += "Error: Distance argument not provided in the set focus distance command.\"}";
                    }
                    break;
                case "setFocusMode":
                    Log.d(TAG, "Received set focus mode command.");
                    if (obj.has("mode")) {
                        String mode = obj.getString("mode");
                        if (mCameraController.setFocusMode(mode)) {
                            commandResult += "Focus mode set to " + mode + ".\"}";
                        } else {
                            commandResult += "Focus mode was either invalid or the camera failed to be configured.\"}";
                        }
                    } else {
                        commandResult += "Error: Mode argument not provided in set focus mode command.\"}";
                    }
                    break;
                case "setPublishImage":
                    Log.d(TAG, "Received set publish image command.");
                    if (obj.has("publish")) {
                        boolean publish = obj.getBoolean("publish");
                        mSciCamPublisher.setPublishImage(publish);
                        if (publish) {
                            commandResult += "Started publishing images!\"}";
                        } else {
                            commandResult += "Stopped publishing images!\"}";
                        }
                    } else {
                        commandResult += "Error: Publish argument not provided in the set publish image command.\"}";
                    }
                    break;
                case "setPublishedImageSize":
                    Log.d(TAG, "Received set published image size command.");
                    if (obj.has("width") && obj.has("height")) {
                        Size imageSize  = new Size(obj.getInt("width"), obj.getInt("height"));
                        if (mSciCamPublisher.setPublishSize(imageSize)) {
                            commandResult += "Publish image size set to " + imageSize.getWidth();
                            commandResult += " by " + imageSize.getHeight() + "!\"}";
                        } else {
                            commandResult += "Image size " + imageSize.getWidth();
                            commandResult += " by " + imageSize.getHeight();
                            commandResult += " is invalid. Height and width must be greater than 0.";
                        }
                    } else {
                        commandResult += "Error: Height and/or width not provided in the set published image size command.\"}";
                    }
                    break;
                case "setPublishedImageType":
                    Log.d(TAG, "Received set published image type command.");
                    if (obj.has("type")) {
                        String imageType = obj.getString("type");
                        if (mSciCamPublisher.setPublishType(imageType)) {
                            commandResult += "Publish image type set to " + imageType + "!\"}";
                        } else {
                            commandResult += "Image type " + imageType;
                            commandResult += " is invalid. It must be color or grayscale.\"}";
                        }
                    } else {
                        commandResult += "Error: Type argument not provided in the set published image type command.\"}";
                    }
                    break;
                case "setSavePicturesToDisk":
                    Log.d(TAG, "Received set save picture to disk command.");
                    if (obj.has("save")) {
                        boolean save = obj.getBoolean("save");
                        mCameraController.setSaveImage(save);
                        if (save) {
                            commandResult += "Started saving images!\"}";
                        } else {
                            commandResult += "Stopped saving images!\"}";
                        }
                    } else {
                        commandResult += "Error: Save argument not provided in the set save pictures to disk command.\"}";
                    }
                    break;
                case "setFocusDistanceFunctionValues":
                    Log.d(TAG, "Received set focal distance function command.");
                    if (obj.has("constant") && obj.has("coefficient")) {
                        float constant = (float)obj.getDouble("constant");
                        float coefficient = (float)obj.getDouble("coefficient");
                        mCameraController.setFocusDistanceFunctionValues(constant, coefficient);
                        commandResult += "Focus Distance function constant set to " + constant;
                        commandResult += " and coefficient set to " + coefficient + "!\"}";
                    } else {
                        commandResult += "Error: Constant and/or coefficient not provided in the set focal distance function values command.\"}";
                    }
                    break;
                default:
                    commandResult += "\"Command " + commandStr + " not recognized!\"}";
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            commandResult += "\"Error parsing JSON!\"}";
        }
        sendData(MessageType.JSON, "data", commandResult);
        Log.i(TAG, commandResult);
    }

    @Override
    public void onGuestScienceStart() {
        Log.i(TAG, "onGuestScienceStart: Starting up the sci cam image apk.");
        // Make sure the apk has permission to use the camera
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "onGuestScienceStart: No camera permissions!");
            sendData(MessageType.JSON, "error", "{\"Summary\": \"No camera permissions!\"}");
            return;
        }

        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        TextureView textureView = new TextureView(getApplicationContext());
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        if (cameraManager == null) {
            Log.e(TAG, "onGuestScienceStart: Unable to get a camera manager.");
            sendData(MessageType.JSON, "error", "{\"Summary\": \"Unable to get a camera manager.\"}");
            return;
        }


        String baseDataPath = getGuestScienceDataBasePath();
        if (baseDataPath.equals("")) {
            baseDataPath = "/sdcard/data/gov.nasa.arc.irg.astrobee.sci_cam_image";
        }

        String dataPath = baseDataPath + File.separator + "delayed";

        mCameraController = new CameraController(windowManager,
                textureView,
                cameraManager,
                dataPath);

        if (!mCameraController.initialize()) {
            sendData(MessageType.JSON, "error", "{\"Summary\": \"Failed to initial camera controller. Check sci cam image apk log for more info.\"}");
            return;
        }

        // Figure out and set focal distance function values
        String robotNameFile = baseDataPath + File.separator + "incoming" + File.separator + "robotname";
        String robotName = "";
        try {
            FileInputStream inputStream = new FileInputStream(robotNameFile);
            robotName = IOUtils.toString(inputStream);
            inputStream.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Failed to find robot name file!", e);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read robot name file!", e);
        }

        if (robotName != "") {
            // Remove any white space from robot name
            robotName = robotName.replaceAll("\\s+","");
            Log.d(TAG, "Robot name is " + robotName);
            boolean found = false;
            String tagName, nameIn, constant, coefficient;
            XmlResourceParser parser = getApplicationContext().getResources().getXml(R.xml.focal_distance_function_values);
            try {
                // Check for end of xml document
                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT && !found) {
                    if (eventType == XmlPullParser.START_TAG) {
                        tagName = parser.getName();
                        if (tagName.contentEquals("robot")) {
                            // Extract name
                            nameIn = parser.getAttributeValue(null, "name");
                            Log.d(TAG, "Read in xml robot name " + nameIn);
                            if (nameIn.equals(robotName)) {
                                found = true;
                                constant = parser.getAttributeValue(null, "constant");
                                coefficient = parser.getAttributeValue(null, "coefficient");
                                Log.d(TAG, "Setting exponent to " + constant + " and coefficient to " + coefficient);
                                mCameraController.setFocusDistanceFunctionValues(Float.parseFloat(constant), Float.parseFloat(coefficient));
                            }
                        }
                    }
                    eventType = parser.next();
                }

                if (!found) {
                    Log.e(TAG, "Unable to find robot name in xml file. Using default focal distance function values.");
                }
            } catch (XmlPullParserException | IOException e) {
                Log.e(TAG, "Unable to parse focal distance function values. Using default values.", e);
            }

        } else {
            Log.e(TAG, "Robot name is empty. Using default focal distance function values.");
        }

        try {
            URI masterURI = new URI("http://llp:11311");

            mNodeConfiguration = NodeConfiguration
                    .newPublic(InetAddressFactory.newNonLoopback().getHostAddress());
            mNodeConfiguration.setMasterUri(masterURI);

            mNodeMainExecutor = DefaultNodeMainExecutor.newDefault();

            mSciCamPublisher = SciCamPublisher.getInstance();

            mNodeMainExecutor.execute(mSciCamPublisher, mNodeConfiguration);
            Log.d(TAG, "Started ROS!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to start the ros connection!", e);
            sendData(MessageType.JSON, "error", "{\"Summary\": \"Failed to start the ros connection!\"}");
            return;
        }


        sendStarted("info");
        Log.i(TAG, "onGuestScienceStart: Sci cam image apk started up successfully!");
    }

    @Override
    public void onGuestScienceStop() {
        mCameraController.setContinuousPictureTaking(false);
        mCameraController.closeCamera();

        mNodeConfiguration = null;
        mNodeMainExecutor = null;
        mSciCamPublisher = null;

        // Inform GDS/the ground that this APK has stopped.
        sendStopped("info");

        stopForeground(true);
        stopSelf();

        // Destroy all connection with the guest science manager.
        terminate();
    }
}
