package gov.nasa.arc.irg.astrobee.sci_cam_image;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraController {

    SciCamImage m_parent;
    public float m_curr_focus_distance;
    
    /**
     * Conversion from screen rotation to JPEG orientation.
     */
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private static final int STATE_PREVIEW = 0;
    private static final int STATE_WAITING_LOCK = 1;
    private static final int STATE_WAITING_PRECAPTURE = 2;
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    private static final int STATE_PICTURE_TAKEN = 4;
    private static final int MAX_PREVIEW_WIDTH = 1920;
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private AutoFitTextureView textureView;

    private String mCameraId;
    private CameraCaptureSession mCaptureSession;
    private CameraDevice mCameraDevice;
    private Size mPreviewSize;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private ImageReader imageReader;
    private File file;

    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private int mState = STATE_PREVIEW;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private boolean mFlashSupported;
    private int mSensorOrientation;

    CameraCharacteristics mCameraCharacteristics;
        
    public CameraController(SciCamImage parent, AutoFitTextureView textureView) {
        this.m_parent = parent;
        this.textureView = textureView;
        this.textureView.setSurfaceTextureListener(mSurfaceTextureListener);
        file = null;
    }
    
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
            
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }

    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            // This method is called when the camera is opened.  We start camera preview here.
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if(m_parent != null) {
                m_parent.finish();
            }
        }

    };

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            backgroundHandler.post(new ImageSaver(m_parent, reader.acquireNextImage()));
        }

    };

    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {
            switch (mState) {
                case STATE_PREVIEW: {
                    // We have nothing to do when the camera preview is working normally.
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);

                    
                    if (afState == null) {
                        //captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                               CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {

                        if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
                            // may want to print something here
                        } else if (CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                            // may want to print something here
                        }
                        
                        // CONTROL_AE_STATE can be null on some devices
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPrecaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE || aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRECAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRECAPTURE: {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(SciCamImage.SCI_CAM_TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) m_parent.getSystemService(Context.CAMERA_SERVICE);
        
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    continue;
                }

                mCameraCharacteristics = characteristics;
                
                int[] capabilities
                    = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);

                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG, "size of capabilities is  " +
                          capabilities.length);
                
                // iterating over an array 
                for (int i = 0; i < capabilities.length; i++) { 
                    // accessing each element of array 
                    int x = capabilities[i]; 
                    if (SciCamImage.doLog)
                        Log.i(SciCamImage.SCI_CAM_TAG, "Capability: " + x);
                }

                if (SciCamImage.doLog) {
                    Log.i(SciCamImage.SCI_CAM_TAG, "The above must be one of: ");
                    Log.i(SciCamImage.SCI_CAM_TAG, "Manual capability:" +
                          CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR);
                    
                    Log.i(SciCamImage.SCI_CAM_TAG, "Backward compatible is  " +
                          CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE);
                
                    int deviceLevel = characteristics.get
                        (CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    
                    Log.i(SciCamImage.SCI_CAM_TAG, "Device level is " + deviceLevel);
                    Log.i(SciCamImage.SCI_CAM_TAG, "The above must be one of: ");
                    
                    Log.i(SciCamImage.SCI_CAM_TAG, "Legacy is "
                          + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY);
                    Log.i(SciCamImage.SCI_CAM_TAG, "Limited is "
                          + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED);
                    Log.i(SciCamImage.SCI_CAM_TAG, "Full "
                          + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
                    Log.i(SciCamImage.SCI_CAM_TAG, "Level 3 is "
                          + CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3);
                }
                
                StreamConfigurationMap map = characteristics.get
                    (CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                // For still image captures, we use the largest available size.
                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                                               new CompareSizesByArea());
                imageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                                                      ImageFormat.JPEG, /*maxImages*/2);
                imageReader.setOnImageAvailableListener(mOnImageAvailableListener,
                                                        backgroundHandler);
                
                // Find out if we need to swap dimension to get the preview size relative to sensor
                // coordinate.
                int displayRotation = m_parent.getWindowManager().getDefaultDisplay().getRotation();
                //getWindowManager().getDefaultDisplay().getRotation();
                
                //noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(SciCamImage.SCI_CAM_TAG, "Display rotation is invalid: " +
                              displayRotation);
                }

                Point displaySize = new Point();
                m_parent.getWindowManager().getDefaultDisplay().getSize(displaySize);
                //getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
                int maxPreviewWidth = displaySize.x;
                int maxPreviewHeight = displaySize.y;

                if (swappedDimensions) {
                    rotatedPreviewWidth = height;
                    rotatedPreviewHeight = width;
                    maxPreviewWidth = displaySize.y;
                    maxPreviewHeight = displaySize.x;
                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }

                // Danger. Attempting to use too large a preview size
                // could exceed the camera bus' bandwidth limitation,
                // resulting in gorgeous previews but the storage of
                // garbage capture data.
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                                                 rotatedPreviewWidth, rotatedPreviewHeight,
                                                 maxPreviewWidth, maxPreviewHeight, largest);

                // We fit the aspect ratio of TextureView to the size of preview we picked.
                int orientation = m_parent.getResources().getConfiguration().orientation;
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    textureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    textureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }

                // Check if the flash is supported.
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;

                mCameraId = cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            e.printStackTrace();
        }
    }

    public void openCamera(int width, int height) {
        if (SciCamImage.doLog)
            Log.i(SciCamImage.SCI_CAM_TAG, "open camera");

        if (ContextCompat.checkSelfPermission(m_parent, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) m_parent.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            startBackgroundThread();
            manager.openCamera(mCameraId, mStateCallback, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    public void closeCamera() {
        try {
            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "close camera");
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != imageReader) {
                imageReader.close();
                imageReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            stopBackgroundThread();
        }
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createCameraPreviewSession() {
        try {
            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "createCameraPreviewSession");

            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;

            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest
                (CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);

            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            // The camera is already closed
                            if (null == mCameraDevice) {
                                return;
                            }

                            // When the session is ready, we start displaying the preview.
                            mCaptureSession = cameraCaptureSession;
                            try {
                                // Auto focus should be continuous for camera preview.
                                if (SciCamImage.doLog)
                                    Log.i(SciCamImage.SCI_CAM_TAG,
                                          "For preview, setting AF mode to continuous pic");
                                
                                mPreviewRequestBuilder.set
                                    (CaptureRequest.CONTROL_AF_MODE,
                                     CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                // Flash is automatically enabled when necessary.
                                setAutoFlash(mPreviewRequestBuilder);

                                // Finally, we start displaying the camera preview.
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                mCaptureSession.setRepeatingRequest(mPreviewRequest,
                                                                    mCaptureCallback,
                                                                    backgroundHandler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed
                            (@NonNull CameraCaptureSession cameraCaptureSession) {
                            Log.e(SciCamImage.SCI_CAM_TAG, "Configuration Failed");
                        }
                    }, null
            );
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (SciCamImage.doLog)
            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "configure transform");
        
        if (null == textureView || null == mPreviewSize) {
            return;
        }
        int rotation = m_parent.getWindowManager().getDefaultDisplay().getRotation();
        //getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        textureView.setTransform(matrix);
    }

    public void takePicture() {
        try {
            Log.i(SciCamImage.SCI_CAM_TAG, "Take picture");

            // This is how to tell the camera to lock focus.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                       CameraMetadata.CONTROL_AF_TRIGGER_START);
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // This is how to tell the camera to trigger.
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                       CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);

            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                                    backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            if (null == mCameraDevice) {
                return;
            }

            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "Capturing still picture");
            
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder
                = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());

            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "Setting focus");

            // captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
            //                   CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            
            if (m_parent.focusMode.equals("auto")) {
                // Set auto focus
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                   CaptureRequest.CONTROL_AF_MODE_AUTO);
            }else{
                // Set manual focus
                captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                   CameraMetadata.CONTROL_AF_MODE_OFF);
            }
            
            if (captureBuilder.get(CaptureRequest.CONTROL_AF_MODE) ==
                CameraMetadata.CONTROL_AF_MODE_OFF) {
                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG, "Success in setting AF mode to off");
            }
            if (captureBuilder.get(CaptureRequest.CONTROL_AF_MODE) ==
                CameraMetadata.CONTROL_AF_MODE_AUTO) {
                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG, "Success in setting AF mode to auto");
            }
            if (captureBuilder.get(CaptureRequest.CONTROL_AF_MODE) ==
                CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG,
                          "Success in setting AF mode to continuous picture");
            }

            // Set auto flash
            setAutoFlash(captureBuilder);

            float minimumLens = mCameraCharacteristics.get
                (CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);

            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "minimal focus distance " + minimumLens);

            //LENS_INFO_AVAILABLE_FOCAL_LENGTHS List of focal lengths
            //for CaptureRequest#LENS_FOCAL_LENGTH that are supported
            //by this camera device. If optical zoom is not supported,
            //this list will only contain a single value corresponding
            //to the fixed focal length of the device. Otherwise, this
            //list will include every focal length supported by the
            //camera device, in ascending order.  Units: Millimeters
            float[] lensDistances = mCameraCharacteristics.get
                (CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
            for (int i = 0; i < lensDistances.length; i++) { 
                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG, "Focal length: " + lensDistances[i]);
            }
            
            // LENS_FOCUS_DISTANCE: Desired distance to plane of
            // sharpest focus, measured from front-most surface of the
            // lens in units of diopters (1/meter), so 0.0f represents
            // focusing at infinity, and increasing positive numbers
            // represent focusing closer and closer to the camera
            // device.
            // This has an effect only for manual focus.
            captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, m_parent.focusDistance);
            if (SciCamImage.doLog)
                Log.i(SciCamImage.SCI_CAM_TAG, "Set the focus distance: " +
                      captureBuilder.get(CaptureRequest.LENS_FOCUS_DISTANCE));
            
            
            // Orientation
            int rotation = textureView.getDisplay().getRotation();
            //getWindowManager().getDefaultDisplay().getRotation();
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));

            CameraCaptureSession.CaptureCallback CaptureCallback
                = new CameraCaptureSession.CaptureCallback() {

                @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                   @NonNull CaptureRequest request,
                                                   @NonNull TotalCaptureResult result) {

                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF state is null "
                                  + afState);
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF state is focus locked "
                                  + afState);
                    } else if (CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF state is not focus locked "
                                  + afState);
                    }else{
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF state is unknown: " + afState);
                    }

                    m_curr_focus_distance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
                    Log.i(SciCamImage.SCI_CAM_TAG, "Used lens focus distance "
                          + m_curr_focus_distance);
                    
                    int val = result.get(CaptureResult.CONTROL_AF_MODE);
                    if (val == CaptureResult.CONTROL_AF_MODE_OFF) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF mode mode is off " + val);
                    }else if (val == CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF mode mode is cont pic " + val);
                    }else if (val == CaptureResult.CONTROL_AF_MODE_CONTINUOUS_VIDEO) {
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF mode mode is cont video " + val);
                    }else{
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "AF mode mode is unknown " + val);
                    }

                    try {
                        // Reset the auto-focus trigger
                        if (SciCamImage.doLog)
                            Log.i(SciCamImage.SCI_CAM_TAG, "Reset AF mode");
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                                                   CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
                        setAutoFlash(mPreviewRequestBuilder);
                        mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                                                backgroundHandler);
                        // After this, the camera will go back to the normal state of preview.
                        mState = STATE_PREVIEW;
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                                                            backgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            };

            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), CaptureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    private void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                               CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    // This function publishes the picture over ROS and can also save it to disk
    private static class ImageSaver implements Runnable {

        // The parent class with which this object communicates
        private SciCamImage m_parent;
        
        // The JPEG image
        private final Image mImage;
        
        public ImageSaver(SciCamImage parent, Image image) {
            this.m_parent = parent;
            mImage = image;
        }

        // Create the output file name. We use a name representing
        // seconds (with fractional part) since epoch, in the same way
        // as ROS timestamps are represented.
        private File getOutputMediaFile(long secs, long nsecs) {

            File mediaStorageDir = new File(m_parent.dataPath);
            
            // Create the storage directory if it does not exist
            if (!mediaStorageDir.exists()) {
                if (!mediaStorageDir.mkdirs()) {
                    return null;
                }
            }
            
            String timeStamp = String.format("%d.%d", secs, nsecs);
            File mediaFile;
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + timeStamp + ".jpg");
            
            return mediaFile;
        }
        
        @Override
        public void run() {

            // First thing, record the time
            Date date = new Date();
            long curr_time = date.getTime();
            long secs = curr_time/1000;
            long nsecs = (curr_time - secs * 1000) * 1000;
            
            // The file we save the image into
            File mediaFile = getOutputMediaFile(secs, nsecs);

            ByteBuffer buffer = mImage.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);

            Integer width = mImage.getWidth();
            Integer height = mImage.getHeight();

            if (SciCamImage.doLog) {
                Log.i(SciCamImage.SCI_CAM_TAG, "Image width is  " + width);
                Log.i(SciCamImage.SCI_CAM_TAG, "Image height is  " + height);
            }

            // Publish
            if (m_parent.sciCamPublisher != null) {
                if (m_parent.previewImageWidth <= 0 || m_parent.previewImageWidth >= width) {
                    // publish at full resolution
                    m_parent.sciCamPublisher.onNewImage(bytes, width, height, secs, nsecs);
                } else {
                    // Publish at reduced resolution
                    int previewWidth = m_parent.previewImageWidth;
                    int previewHeight = (int)Math.round( (double)height * (double)previewWidth
                                                         / (double)width);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Bitmap preview_bitmap
                        = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    preview_bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                    m_parent.sciCamPublisher.onNewImage(outStream.toByteArray(),
                                                        previewWidth, previewHeight, secs, nsecs);
                }
            }
            
            FileOutputStream output = null;
            try {
                if (m_parent.savePicturesToDisk) {
                    // Save to disk
                    output = new FileOutputStream(mediaFile);
                    Log.i(SciCamImage.SCI_CAM_TAG, "Writing: " + mediaFile.toString());
                    output.write(bytes);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (SciCamImage.doLog)
                    Log.i(SciCamImage.SCI_CAM_TAG, "Closing image");
                // This is very important, otherwise the app will crash next time around
                mImage.close();
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            
            // Protect variables used in the other thread
            synchronized (m_parent){
                // If only one picture is needed, declare it taken
                if (m_parent.takeSinglePicture)
                    m_parent.takeSinglePicture = false;

                if (SciCamImage.doLog) {
                    Log.i(SciCamImage.SCI_CAM_TAG, "Camera no longer in use");
                }
                m_parent.inUse = false; // done processing the picture
            }
        }
    }

    static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth()
                               * rhs.getHeight());
        }

    }
}

