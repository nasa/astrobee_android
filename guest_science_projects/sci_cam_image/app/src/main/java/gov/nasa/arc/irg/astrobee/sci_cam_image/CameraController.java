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
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Size;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CameraController {
    private boolean m_autoExposure = true;
    // private boolean m_cameraInUse = false;
    private boolean m_cameraOpen = false;
    private boolean m_captureTimerRunning = false;
    private boolean m_flashSupported = false;
    private boolean m_saveImage = true;

    private CameraCaptureSession.CaptureCallback m_captureCallback = new CameraCaptureSession.CaptureCallback() {
        public void onCaptureBufferLost(CameraCaptureSession session, CaptureRequest request, Surface target, long frameNumber) {
            Log.e(StartSciCamImage.TAG, "onCaptureBufferLost: What does this mean?");
        }

        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d(StartSciCamImage.TAG, "onCaptureCompleted: Capture completed with result: " + result.getPartialResults());
        }

        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Log.e(StartSciCamImage.TAG, "onCaptureFailed: Capture failed with " + failure.getReason());
        }

        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            Log.e(StartSciCamImage.TAG, "onCaptureSequenceAborted: sequence id is " + sequenceId);
        }
    };

    private CameraDevice m_cameraDevice = null;

    private final CameraDevice.StateCallback m_cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NotNull CameraDevice cameraDevice) {
            Log.i(StartSciCamImage.TAG, "onOpened: camera opened.");
            m_cameraDevice = cameraDevice;

            try {
                m_surfaceTexture = m_textureView.getSurfaceTexture();
                if (m_surfaceTexture == null) {
                    Log.e(StartSciCamImage.TAG, "onOpened: Surface texture is null!");
                    return;
                }

                m_imageSurface = new Surface(m_surfaceTexture);

                m_cameraDevice.createCaptureSession(
                        Arrays.asList(m_imageSurface, m_reader.getSurface()),
                        m_captureStateCallback,
                        m_captureHandler);
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
        }

        @Override
        public void onError(@NotNull CameraDevice cameraDevice, int error) {
            Log.e(StartSciCamImage.TAG, "onError: camera device error: " + error);
            // TODO(Katie) Stop capture timer? Close camera ?
        }
    };

    private CameraManager m_cameraManager = null;

    private CaptureRequest.Builder m_captureBuilder = null;

    private final CaptureStateCallback m_captureStateCallback = new CaptureStateCallback();

    private double m_captureRate = 0.50;

    private float m_focusDistance = 0.39f;

    private Handler m_captureHandler = null;
    private Handler m_imageHandler = null;
    private final HandlerThread m_captureThread = new HandlerThread("CaptureThread");
    private final HandlerThread m_imageThread = new HandlerThread("ImageThread");

    private ImageReader m_reader = null;

    // private ReentrantLock m_cameraInUseLock = new ReentrantLock();

    private Size m_captureSize;

    private String m_dataPath = "";
    private String m_focusMode = "manual";

    private Surface m_imageSurface = null;
    private SurfaceTexture m_surfaceTexture = null;

    private TextureView m_textureView = null;

    private  final TextureView.SurfaceTextureListener m_surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            Log.i(StartSciCamImage.TAG, "onSurfaceTextureAvailable: Opening the camera.");
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

    private Timer m_captureTimer = null;

    private WindowManager m_windowManager = null;

    class CaptureImageTask extends TimerTask {
        public void run() {
            captureImage();
            /*m_cameraInUseLock.lock();
            if (!m_cameraInUse) {
                captureImage();
            } else {
                Log.d(StartSciCamImage.TAG, "run: The camera is still trying to capture the last image.");
            }
            m_cameraInUseLock.unlock();*/
        }
    }

    final class CaptureStateCallback extends CameraCaptureSession.StateCallback {
        public CameraCaptureSession m_captureSession = null;

        @Override
        public void onConfigured(@NotNull CameraCaptureSession session) {
            Log.i(StartSciCamImage.TAG, "onConfigured: session configured.");
            m_captureSession = session;

            try {
                CaptureRequest.Builder builder =
                        m_captureSession.getDevice()
                                .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                builder.addTarget(m_imageSurface);
                //builder.addTarget(m_reader.getSurface());
                m_captureSession.setRepeatingRequest(builder.build(), null, m_captureHandler);
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
            m_captureSession = null;
        }
    }

    public CameraController(WindowManager windowManager,
                            TextureView textureView,
                            CameraManager cameraManager,
                            String dataPath) {
        m_windowManager = windowManager;
        m_textureView = textureView;
        m_cameraManager = cameraManager;
        m_dataPath = dataPath;

        m_captureSize = new Size(5344, 4008);
    }

    public void captureImage() {
        try {
            // The camera lock gets locked in the calling function
            //m_cameraInUse = true;
            Log.d(StartSciCamImage.TAG, "captureImage: Sending capture request to camera.");
            if (m_captureBuilder == null) {
                if (!initializeCaptureBuilder()) {
                    Log.e(StartSciCamImage.TAG, "captureImage: Unable to initialize capture " +
                            "builder! Unable to capture image!");
                }
            }
            m_captureStateCallback.m_captureSession.capture(m_captureBuilder.build(), m_captureCallback, m_captureHandler);

            /*CaptureRequest.Builder builder =
                    m_captureStateCallback.m_captureSession.getDevice()
                        .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            builder.addTarget(m_reader.getSurface());
            m_captureStateCallback.m_captureSession.capture(builder.build(), null, m_captureHandler);*/
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "captureImage: Unable to create capture request. Exception: ", e);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "captureImage: Exception when ", e);
        }
    }

    public void closeCamera() {
        Log.d(StartSciCamImage.TAG, "closeCamera: Closing camera.");
        if (m_captureTimerRunning) {
            stopCaptureTimer();
        }
        if (m_cameraDevice != null) {
            m_cameraDevice.close();
            m_cameraDevice = null;
        }
        if (m_reader != null) {
            m_reader.close();
            m_reader = null;
        }
        if (m_captureBuilder != null) {
            m_captureBuilder = null;
        }
    }

    public double getCaptureRate() {
        return m_captureRate;
    }

    public boolean getCaptureTimerRunning() {

        return m_captureTimerRunning;
    }

    public boolean initialize() {
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(1, 1,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.LEFT | Gravity.TOP;

        m_textureView.setSurfaceTextureListener(m_surfaceTextureListener);

        try {
            m_windowManager.addView(m_textureView, layoutParams);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initialize: Add view failed!", e);
            return false;
        }

        m_captureThread.start();
        m_captureHandler = new Handler(m_captureThread.getLooper());

        m_imageThread.start();
        m_imageHandler = new Handler(m_imageThread.getLooper());

        m_reader = ImageReader.newInstance(m_captureSize.getWidth(),
                                           m_captureSize.getHeight(),
                                           ImageFormat.JPEG,
                                           10);

        m_reader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                final Image image = reader.acquireLatestImage();
                Log.d(StartSciCamImage.TAG, "onImageAvailable: Acquired image at " + image.getTimestamp());
                m_imageHandler.post(new ImageHandler(image, m_saveImage, m_dataPath));
               /* m_cameraInUseLock.lock();
                m_cameraInUse = false;
                m_cameraInUseLock.unlock();*/
            }
        }, m_captureHandler);

        return true;
    }

    public boolean initializeCaptureBuilder() {
        try {
            m_captureBuilder =
                    m_captureStateCallback.m_captureSession.getDevice()
                            .createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            m_captureBuilder.addTarget(m_reader.getSurface());

            if (!setFocusDistance(m_focusDistance)) {
                m_captureBuilder = null;
                return false;
            }

            if (!setFocusMode(m_focusMode)) {
                m_captureBuilder = null;
                return false;
            }

            if (m_flashSupported) {
                m_captureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                                     CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH);
                if (m_captureBuilder.get(CaptureRequest.CONTROL_AE_MODE) ==
                        CameraMetadata.CONTROL_AE_MODE_ON_AUTO_FLASH) {
                    Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: flash turned on!");
                } else {
                    // We don't care if the flash wasn't turned on
                    Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Failed to configure " +
                            "the flash on the camera even though flash is available.");
                }
            }

            Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Control mode is set to: " +
                    m_captureBuilder.get(CaptureRequest.CONTROL_MODE));
            Log.d(StartSciCamImage.TAG, "initializeCaptureBuilder: Control AE mode is set to: " +
                    m_captureBuilder.get(CaptureRequest.CONTROL_AE_MODE));
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initializeCaptureBuilder: Unable to create a capture builder!", e);
            m_captureBuilder = null;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(StartSciCamImage.TAG, "initialieCaptureBuilder: An exception occurred when creating a capture builder!", e);
            m_captureBuilder = null;
            return false;
        }
        return true;
    }

    public boolean isCameraOpen() {
        return m_cameraOpen;
    }

    public void openCamera() {
        m_cameraOpen = false;
        try {
            String[] ids = m_cameraManager.getCameraIdList();
            Log.d(StartSciCamImage.TAG, "openCamera: Number of camera ids: " + ids.length);
            Log.d(StartSciCamImage.TAG, "openCamera: Camera array string: " + Arrays.toString(ids));

            CameraCharacteristics cc = m_cameraManager.getCameraCharacteristics(ids[0]);
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

            Size[] outputSizes = map.getOutputSizes(ImageFormat.JPEG);
            for (int i = 0; i < outputSizes.length; i++) {
                Log.d(StartSciCamImage.TAG, "openCamera: output size option " + i + ": width: " + outputSizes[i].getWidth() + " height: " + outputSizes[i].getHeight());
            }

            // Check if flash is supported
            if (cc.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)) {
                m_flashSupported = true;
                Log.d(StartSciCamImage.TAG, "openCamera: Flash is supported.");
            } else {
                m_flashSupported = false;
                Log.d(StartSciCamImage.TAG, "openCamera: Flash is not supported.");
            }

            m_cameraManager.openCamera(ids[0], m_cameraStateCallback, null);
            m_cameraOpen = true;
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
        m_autoExposure = auto;

        if (m_autoExposure) {
            autoExposureKey = CameraMetadata.CONTROL_AE_MODE_ON;
        } else {
            autoExposureKey = CameraMetadata.CONTROL_AE_MODE_OFF;
        }

        if (m_captureBuilder != null) {
            m_captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, autoExposureKey);

           if (m_captureBuilder.get(CaptureRequest.CONTROL_AE_MODE) == autoExposureKey) {
               Log.d(StartSciCamImage.TAG, "Auto exposure set to " + autoExposureKey + " as requested.");
           } else {
               Log.d(StartSciCamImage.TAG, "Failed to set auto exposure to " + autoExposureKey +
                       ". It is currently set to "
                       + m_captureBuilder.get(CaptureRequest.CONTROL_AE_MODE) + ".");
               return false;
           }
        }

        return true;
    }

    public boolean setCaptureRate(double rate) {
        if (rate > 0) {
            if (rate != m_captureRate) {
                m_captureRate = rate;

                if (m_captureTimerRunning) {
                    startCaptureTimer();
                }
            } else {
                Log.i(StartSciCamImage.TAG, "setCaptureRate: Capture rate is already set to " + rate + ".");
            }
            return true;
        } else {
            Log.e(StartSciCamImage.TAG, "setCaptureRate: Capture rate cannot by less than 0! Rate was: " + rate + ".");
        }
        return false;
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
            m_focusDistance = focusDistance;
            if (m_captureBuilder != null) {
                m_captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE,
                        focusDistance);
                // Make sure the focus distance was set
                if (m_captureBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE) == focusDistance) {
                    Log.i(StartSciCamImage.TAG, "Camera focus distance successfully set to " +
                            focusDistance + ".");
                } else {
                    Log.e(StartSciCamImage.TAG, "Failed to set camera focus distance. It is currently set to "
                            + m_captureBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE) + ".");
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
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
        m_focusMode = focusMode;

        if (m_captureBuilder != null) {
            m_captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, modeKey);

            if (m_captureBuilder.get(CaptureRequest.CONTROL_AF_MODE) == modeKey) {
                Log.d(StartSciCamImage.TAG, "Focus mode set to " + modeKey + " as requested.");
            } else {
                Log.d(StartSciCamImage.TAG, "Failed to set focus mode to " + modeKey +
                        ". It is currently set to "
                        + m_captureBuilder.get(CaptureRequest.CONTROL_AF_MODE) + ".");
                return false;
            }
        }

        return true;
    }

    public void setSaveImage(boolean save) {
        m_saveImage = save;
    }

    public void startCaptureTimer() {
        // Check if the timer is already running. If it is, we need to cancel it.
        if (m_captureTimerRunning) {
            m_captureTimer.cancel();
        }

        // Apparently, timers can only be used once so we have create a new one each time.
        m_captureTimer = new Timer();
        // Convert seconds to milliseconds
        m_captureTimer.schedule(new CaptureImageTask(), 0, (long) (m_captureRate * 1000));
        m_captureTimerRunning = true;
    }

    public void stop() {
        stopCaptureTimer();
        closeCamera();
    }

    public void stopCaptureTimer() {
        if (m_captureTimer != null) {
            m_captureTimer.cancel();
        }
        m_captureTimerRunning = false;
    }
}
