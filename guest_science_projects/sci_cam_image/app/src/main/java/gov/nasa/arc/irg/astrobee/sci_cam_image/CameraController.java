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

import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CameraController {
    private boolean mAutoExposure = true;
    private boolean mCameraInUse = false;
    private boolean mCameraOpen = false;
    private boolean mContinuousPictureTaking = false;
    private boolean mFlashSupported = false;
    private boolean mSaveImage = true;

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
            Log.e(StartSciCamImage.TAG, "onCaptureBufferLost: What does this mean?");
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(StartSciCamImage.TAG, "onCaptureCompleted: Capture completed with sequence id: " + result.getSequenceId());

            Date date = new Date();
            mCaptureCompleteTimestamp = date.getTime();
        }

        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Log.e(StartSciCamImage.TAG, "onCaptureFailed: Capture failed with " + failure.getReason());
        }

        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            Log.e(StartSciCamImage.TAG, "onCaptureSequenceAborted: sequence id is " + sequenceId);
        }
    };

    private CameraDevice mCameraDevice = null;

    private final CameraDevice.StateCallback mCameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NotNull CameraDevice cameraDevice) {
            Log.i(StartSciCamImage.TAG, "onOpened: camera opened.");
            mCameraDevice = cameraDevice;

            try {
                mSurfaceTexture = mTextureView.getSurfaceTexture();
                if (mSurfaceTexture == null) {
                    Log.e(StartSciCamImage.TAG, "onOpened: Surface texture is null!");
                    return;
                }

                mImageSurface = new Surface(mSurfaceTexture);

                mCameraDevice.createCaptureSession(
                        Arrays.asList(mImageSurface, mReader.getSurface()),
                        mCaptureStateCallback,
                        mCaptureHandler);
                mCameraOpen = true;
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(StartSciCamImage.TAG, "onOpened: Camera access exception when trying to create capture session.", e);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(StartSciCamImage.TAG, "onOpened: Exception when ", e);
            }
        }

        @Override
        public void onDisconnected(@NotNull CameraDevice cameraDevice) {
            Log.i(StartSciCamImage.TAG, "onDisconnected: camera disconnected.");
            if (mContinuousPictureTaking) {
                Log.e(StartSciCamImage.TAG, "Stopping continuous picture taking.");
                mContinuousPictureTaking = false;
            }
        }

        @Override
        public void onError(@NotNull CameraDevice cameraDevice, int error) {
            Log.e(StartSciCamImage.TAG, "onError: camera device error: " + error);
            if (mContinuousPictureTaking) {
                Log.e(StartSciCamImage.TAG, "Stopping continuous picture taking.");
                mContinuousPictureTaking = false;
            }
        }
    };

    private CameraManager mCameraManager = null;

    private CaptureRequest.Builder mCaptureBuilder = null;

    private final CaptureStateCallback mCaptureStateCallback = new CaptureStateCallback();

    private float mFocusDistance = 0.39f;
    private float mFocusDistanceFunctionCoefficient = 5.76f;
    private float mFocusDistanceFunctionConstant = -7.56f;

    private Handler mCaptureHandler = null;

    private final HandlerThread mCaptureThread = new HandlerThread("CaptureThread");

    private ImageReader mReader = null;

    private int mNumImagesToDiscard = 0;

    private int mNumDiscardImagesWhenChangingFocus = 1;

    private long mAverageTimeBetweenImages = 1700;
    private long mCaptureCompleteTimestamp;
    long mCaptureTimeout = mAverageTimeBetweenImages * 3;

    private SciCamPublisher mSciCamPublisher;

    private Size mCaptureSize;

    private String mDataPath = "";
    private String mFocusMode = "manual";

    private Surface mImageSurface = null;
    private SurfaceTexture mSurfaceTexture = null;

    private TextureView mTextureView = null;

    private  final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(StartSciCamImage.TAG, "onSurfaceTextureAvailable: Opening the camera.");
            // Have to wait until the surface is available before opening the camera. Part of
            // opening the camera requires the surface and if the surface isn't available, the
            // apk will crash.
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };

    private Timer mCaptureTimeoutTimer = null;

    private WindowManager mWindowManager = null;

    final class CaptureStateCallback extends CameraCaptureSession.StateCallback {
        public CameraCaptureSession mCaptureSession = null;

        @Override
        public void onConfigured(@NotNull CameraCaptureSession session) {
            Log.i(StartSciCamImage.TAG, "onConfigured: session configured.");
            mCaptureSession = session;

            try {
                // This capture request of a preview is what gets displayed to the screen.
                // Without this repeating request, the still image captures will be black.
                CaptureRequest.Builder builder =
                        mCaptureSession.getDevice()
                                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(mImageSurface);
                mCaptureSession.setRepeatingRequest(builder.build(), null, mCaptureHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
                Log.e(StartSciCamImage.TAG, "onConfigured: Camera access exception occurred when trying to create capture request.", e);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(StartSciCamImage.TAG, "onConfigured: Exception occurred when trying to create capture request.", e);
            }
        }

        @Override
        public void onReady(CameraCaptureSession session) {
            // This method is called every time the session has no more capture requests to process
        }

        @Override
        public void onConfigureFailed(@NotNull CameraCaptureSession session) {
            Log.e(StartSciCamImage.TAG, "onConfigureFailed: session configuration failed!");
            mCaptureSession = null;
        }
    }

    public CameraController(WindowManager windowManager,
                            TextureView textureView,
                            CameraManager cameraManager,
                            String dataPath) {
        mWindowManager = windowManager;
        mTextureView = textureView;
        mCameraManager = cameraManager;
        mDataPath = dataPath;

        // This is the max size jpeg that the camera can capture
        mCaptureSize = new Size(5344, 4008);

    }

    // Lab tested distance to focal distance values
    // 0.2  --> 15.0
    // 0.25 --> 11.5
    // 0.28 --> 10.0
    // 0.3  -->  9.0
    // 0.35 -->  6.5
    // 0.4  -->  5.5
    // 0.5  -->  4.5
    // 0.6  -->  3.25

    public void startManualFocusCapture(float hazCamDistance) {
        // Function found using lab data. For more information, see:
        // https://babelfish.arc.nasa.gov/confluence/display/astrosoft/ISAAC+Close-up+Inspection?focusedTaskId=251
        float newFocusDistance = (float) ((mFocusDistanceFunctionCoefficient/hazCamDistance) + mFocusDistanceFunctionConstant);
        Log.d(StartSciCamImage.TAG, "startManualFocusCapture: Haz cam distance: " + hazCamDistance);
        Log.d(StartSciCamImage.TAG, "startManualFocusCapture: Calculated focus distance: " + newFocusDistance);
        // Make sure the focus mode is set to manual and the focus distance is set correctly
        if (newFocusDistance != mFocusDistance || mFocusMode != "manual") {
            // Set focus mode
            setFocusMode("manual");
            // Set focus distance
            setFocusDistance(newFocusDistance);
            mNumImagesToDiscard = mNumDiscardImagesWhenChangingFocus;
        }
        captureImage();
    }

    public void captureImage() {
        try {
            mCameraInUse = true;
            Log.d(StartSciCamImage.TAG, "captureImage: Sending capture request to camera.");
            if (mCaptureBuilder == null) {
                if (!initializeCaptureBuilder()) {
                    Log.e(StartSciCamImage.TAG, "captureImage: Unable to initialize capture " +
                            "builder! Unable to capture image!");
                }
            }

            Log.d(StartSciCamImage.TAG, "captureImage: stop repeating and aborting captures.");
            // Without these functions, the camera seems to freeze somewhere between
            // onCaptureComplete and onImageAvailable
            mCaptureStateCallback.mCaptureSession.stopRepeating();
            mCaptureStateCallback.mCaptureSession.abortCaptures();
            mCaptureStateCallback.mCaptureSession.capture(mCaptureBuilder.build(), mCaptureCallback, mCaptureHandler);
            // Android/Java doesn't let you reuse timers so we have to make a new one each time
            mCaptureTimeoutTimer = new Timer();
            mCaptureTimeoutTimer.schedule(new CaptureTimeoutTask(), mCaptureTimeout);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "captureImage: Unable to create capture request. Exception: ", e);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "captureImage: Exception when ", e);
        }
    }


    class CaptureTimeoutTask extends TimerTask {
        public void run() {
            Log.d(StartSciCamImage.TAG, "run: In capture timeout. Something went wrong.");
            if (mContinuousPictureTaking == true) {
                mContinuousPictureTaking = false;
                Log.d(StartSciCamImage.TAG, "run: Continuous picture taking failed.");
            } else {
                Log.d(StartSciCamImage.TAG, "run: Failed to take a picture.");
            }
            Log.d(StartSciCamImage.TAG, "run: Sending restart guest science apk command after capture timeout occurred.");
            mSciCamPublisher.publishRestartCommand();
        }
    }

    public void closeCamera() {
        Log.d(StartSciCamImage.TAG, "closeCamera: Closing camera.");
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mReader != null) {
            mReader.close();
            mReader = null;
        }
        if (mCaptureBuilder != null) {
            mCaptureBuilder = null;
        }
    }

    public boolean getCameraInUse() {
        return mCameraInUse;
    }

    public boolean getContinuousPictureTaking() {
        return mContinuousPictureTaking;
    }

    public File getOutputDataFile(long imageTimestamp) {
        File dataStorageDir = new File(mDataPath);

        if (dataStorageDir == null) {
            Log.e(StartSciCamImage.TAG, "getOutputDataFile: data storage directory is null.");
            return null;
        }

        // Create the storage directory if it does not exist
        if (!dataStorageDir.exists()) {
            if (!dataStorageDir.mkdirs()) {
                return null;
            }
        }

        long secs = imageTimestamp/1000;
        long msecs = imageTimestamp % 1000;
        String timestamp = String.format("%d.%03d", secs, msecs);
        return new File(dataStorageDir + File.separator + timestamp + ".jpg");
    }

    public boolean initialize() {
        mSciCamPublisher = SciCamPublisher.getInstance();

        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);

        try {
            mWindowManager.addView(mTextureView, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initialize: Add view failed!", e);
            return false;
        }

        mCaptureThread.start();
        mCaptureHandler = new Handler(mCaptureThread.getLooper());

        mReader = ImageReader.newInstance(mCaptureSize.getWidth(),
                                           mCaptureSize.getHeight(),
                                           ImageFormat.JPEG,
                                           10);

        mReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                final Image image = reader.acquireLatestImage();
                Date date = new Date();
                // The image get timestamp function returns the nanoseconds since boot time of the
                // device
                long imageTimestamp = (date.getTime() - SystemClock.uptimeMillis()) + (image.getTimestamp() / 1000000);
                Log.d(StartSciCamImage.TAG, "onImageAvailable: Capture complete timestamp: " + mCaptureCompleteTimestamp);
                Log.d(StartSciCamImage.TAG, "onImageAvailable: Image timestamp used: " + imageTimestamp);

                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);

                // Check to see if we are discarding images. If not, save and publish the image.
                if (mNumImagesToDiscard <= 0) {
                    Size imageSize = new Size(image.getWidth(), image.getHeight());

                    if (mSaveImage) {
                        // Image file
                        File imageFile = getOutputDataFile(imageTimestamp);
                        FileOutputStream outputStream = null;
                        if (imageFile != null) {
                            try {
                                Log.d(StartSciCamImage.TAG, "onImageAvailable: Writing image to file: " + imageFile);
                                Log.d(StartSciCamImage.TAG, "onImageAvailable: Focus Distance " + mFocusDistance + "  constant: "
                                        + mFocusDistanceFunctionConstant + " coefficient " + mFocusDistanceFunctionCoefficient
                                        + " used to take image " + imageFile);
                                outputStream = new FileOutputStream(imageFile);
                                outputStream.write(bytes);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e(StartSciCamImage.TAG, "onImageAvailable: Error saving image!", e);
                            } finally {
                                if (outputStream != null) {
                                    try {
                                        outputStream.close();
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Log.d(StartSciCamImage.TAG, "onImageAvailable: Error closing output stream!", e);
                                    }
                                }
                            }
                        }
                    }
                    // Publishing the camera info/image triggers the inspection node to send another
                    // take image command. Writing the image to a file takes longer than it does to
                    // receive this command so need to publish the image after writing it to a file.
                    mSciCamPublisher.publishImage(bytes, imageSize, imageTimestamp);
                }

                Log.d(StartSciCamImage.TAG, "onImageAvailable: Closing image!");
                // This is very important, otherwise the app will crash next time around
                image.close();
                mCaptureTimeoutTimer.cancel();
                mCameraInUse = false;
                if (mContinuousPictureTaking || mNumImagesToDiscard > 0) {
                    captureImage();
                    if (mNumImagesToDiscard > 0) {
                        mNumImagesToDiscard -= 1;
                        Log.d(StartSciCamImage.TAG, "onImageAvailable: " + mNumImagesToDiscard + " still need to be discarded.");
                    }
                }
            }
        }, mCaptureHandler);

        return true;
    }

    public boolean initializeCaptureBuilder() {
        try {
            mCaptureBuilder =
                    mCaptureStateCallback.mCaptureSession.getDevice()
                            .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureBuilder.addTarget(mReader.getSurface());

            if (!setFocusDistance(mFocusDistance)) {
                mCaptureBuilder = null;
                return false;
            }

            if (!setFocusMode(mFocusMode)) {
                mCaptureBuilder = null;
                return false;
            }

            if (mFlashSupported) {
                mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                    CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                if (mCaptureBuilder.get(CaptureRequest.CONTROL_AE_MODE) ==
                        CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH) {
                    Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: flash turned on!");
                } else {
                    // We don't care if the flash wasn't turned on
                    Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Failed to configure " +
                            "the flash on the camera even though flash is available.");
                }
            }

            Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Control mode is set to: " +
                    mCaptureBuilder.get(CaptureRequest.CONTROL_MODE));
            Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Control AE mode is set to: " +
                    mCaptureBuilder.get(CaptureRequest.CONTROL_AE_MODE));
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initializeCaptureBuilder: Unable to create a capture builder!", e);
            mCaptureBuilder = null;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initialieCaptureBuilder: An exception occurred when creating a capture builder!", e);
            mCaptureBuilder = null;
            return false;
        }
        return true;
    }

    public boolean isCameraOpen() {
        return mCameraOpen;
    }

    public void openCamera() {
        mCameraOpen = false;
        try {
            String[] ids = mCameraManager.getCameraIdList();
            Log.d(StartSciCamImage.TAG, "openCamera: Number of camera ids: " + ids.length);
            Log.d(StartSciCamImage.TAG, "openCamera: Camera array string: " + Arrays.toString(ids));

            CameraCharacteristics cc = mCameraManager.getCameraCharacteristics(ids[0]);
            List<CameraCharacteristics.Key<?>> keys = cc.getKeys();
            for (CameraCharacteristics.Key<?> key : keys) {
                Log.d(StartSciCamImage.TAG, "openCamera: " + key.getName() + ": " + cc.get(key));
            }
            int[] caps = cc.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            Log.d(StartSciCamImage.TAG, "openCamera: Name of available capabilities: " + CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES.getName());
            Log.d(StartSciCamImage.TAG, "openCamera: Array of available capabilities: " + Arrays.toString(caps));

            StreamConfigurationMap map = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Log.d(StartSciCamImage.TAG, "openCamera: ImageReader supported " + map.isOutputSupportedFor(ImageReader.class));
            Log.d(StartSciCamImage.TAG, "openCamera: SurfaceHolder supported: " + map.isOutputSupportedFor(SurfaceHolder.class));
            Log.d(StartSciCamImage.TAG, "openCamera: YUV_420_888 supported: " + map.isOutputSupportedFor(ImageFormat.YUV_420_888));
            Log.d(StartSciCamImage.TAG, "openCamera: JPEG supported: " + map.isOutputSupportedFor(ImageFormat.JPEG));
            Log.d(StartSciCamImage.TAG, "openCamera: RAW sensor supported: " + map.isOutputSupportedFor(ImageFormat.RAW_SENSOR));
            Log.d(StartSciCamImage.TAG, "openCamera: 10-bit raw format supported: " + map.isOutputSupportedFor(ImageFormat.RAW10));
            Log.d(StartSciCamImage.TAG, "openCamera: 12-bit raw format supported: " + map.isOutputSupportedFor(ImageFormat.RAW12));

            int[] outputFormats = map.getOutputFormats();
            for (int i = 0; i < outputFormats.length; i++) {
                Log.d(StartSciCamImage.TAG, "openCamera: output format " + outputFormats[i]);
            }

            Size[] outputSizesJpeg = map.getOutputSizes(ImageFormat.JPEG);
            for (int i = 0; i < outputSizesJpeg.length; i++) {
                Log.d(StartSciCamImage.TAG, "openCamera: output size option JPEG: " + i + ": width: " + outputSizesJpeg[i].getWidth() + " height: " + outputSizesJpeg[i].getHeight());
            }

            Size[] outputSizesRaw = map.getOutputSizes(ImageFormat.RAW10);
            for (int i = 0; i < outputSizesRaw.length; i++) {
                Log.d(StartSciCamImage.TAG, "openCamera: output size option raw 10: " + i + ": width: " + outputSizesRaw[i].getWidth() + " height: " + outputSizesRaw[i].getHeight());
            }

            // Check if flash is supported
            if (cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                mFlashSupported = true;
                Log.d(StartSciCamImage.TAG, "openCamera: Flash is supported.");
            } else {
                mFlashSupported = false;
                Log.d(StartSciCamImage.TAG, "openCamera: Flash is not supported.");
            }

            Log.d(StartSciCamImage.TAG, "openCamera: Timeset source is set to " +
                    cc.get(CameraCharacteristics.SENSOR_INFO_TIMESTAMP_SOURCE));

            mCameraManager.openCamera(ids[0], mCameraStateCallback, mCaptureHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "openCamera: Camera access exception when querying and opening the camera.", e);
        } catch (SecurityException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "openCamera: Security exception which probably means that the camera permissions weren't set.", e);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "openCamera: Exception when ", e);
        }
    }

    public boolean setAutoExposure(boolean auto) {
        int autoExposureKey = 0;
        mAutoExposure = auto;

        // Store the auto exposure preference in case the capture builder isn't
        // initialized yet and we need to set the auto exposure later
        if (mAutoExposure) {
            autoExposureKey = CameraMetadata.CONTROL_AE_MODE_ON;
        } else {
            autoExposureKey = CameraMetadata.CONTROL_AE_MODE_OFF;
        }

        if (mCaptureBuilder != null) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE, autoExposureKey);

           if (mCaptureBuilder.get(CaptureRequest.CONTROL_AE_MODE) == autoExposureKey) {
               Log.d(StartSciCamImage.TAG, "Auto exposure set to " + autoExposureKey + " as requested.");
           } else {
               Log.d(StartSciCamImage.TAG, "Failed to set auto exposure to " + autoExposureKey +
                       ". It is currently set to "
                       + mCaptureBuilder.get(CaptureRequest.CONTROL_AE_MODE) + ".");
               return false;
           }
        }

        return true;
    }

    public void setContinuousPictureTaking(boolean continuous) {
        mContinuousPictureTaking = continuous;
    }

    // LENS_FOCUS_DISTANCE: Desired distance to plane of
    // sharpest focus, measured from front-most surface of the
    // lens in units of diopters (1/meter), so 0.0f represents
    // focusing at infinity, and increasing positive numbers
    // represent focusing closer and closer to the camera
    // device.
    public boolean setFocusDistance(float focusDistance) {
        // This has an effect only for manual focus.
        if (focusDistance >= 0) {
            // Store the focus distance in case the capture builder isn't
            // initialized yet and we need to set the focus distance later
            mFocusDistance = focusDistance;
            if (mCaptureBuilder != null) {
                mCaptureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,
                        focusDistance);
                // Make sure the focus distance was set
                if (mCaptureBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE) == focusDistance) {
                    Log.i(StartSciCamImage.TAG, "Camera focus distance successfully set to " +
                            focusDistance + ".");
                } else {
                    Log.e(StartSciCamImage.TAG, "Failed to set camera focus distance. It is currently set to "
                            + mCaptureBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE) + ".");
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    public void setFocusDistanceFunctionValues(float constantIn, float coefficientIn) {
        mFocusDistanceFunctionConstant = constantIn;
        mFocusDistanceFunctionCoefficient = coefficientIn;
    }

    public boolean setFocusMode(String focusMode) {
        int modeKey = 0;
        if (focusMode.equals("manual")) {
            modeKey = CameraMetadata.CONTROL_AF_MODE_OFF;
        } else if (focusMode.equals("auto")) {
            modeKey = CameraMetadata.CONTROL_AF_MODE_AUTO;
        } else {
            return false;
        }

        // Store the focus mode in case the capture builder isn't
        // initialized yet and we need to set the focus mode later
        mFocusMode = focusMode;

        if (mCaptureBuilder != null) {
            mCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE, modeKey);

            if (mCaptureBuilder.get(CaptureRequest.CONTROL_AF_MODE) == modeKey) {
                Log.d(StartSciCamImage.TAG, "Focus mode set to " + modeKey + " as requested.");
            } else {
                Log.d(StartSciCamImage.TAG, "Failed to set focus mode to " + modeKey +
                        ". It is currently set to "
                        + mCaptureBuilder.get(CaptureRequest.CONTROL_AF_MODE) + ".");
                return false;
            }
        }

        return true;
    }

    public void setSaveImage(boolean save) {
        mSaveImage = save;
    }
}
