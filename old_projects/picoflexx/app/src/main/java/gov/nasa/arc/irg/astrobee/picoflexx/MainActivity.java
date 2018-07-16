

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

package gov.nasa.arc.irg.astrobee.picoflexx;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.android.RosActivity;
import org.ros.message.MessageFactory;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import sensor_msgs.PointField;

public class MainActivity extends RosActivity {
    private static final String TAG = "MainActivity";

    static {
        System.loadLibrary("royale");
        System.loadLibrary("picoflexx");
    }

    private static URI ROS_MASTER_URI = Settings.getInstance().getRosMasterUri();
    private static String ROS_HOSTNAME = Settings.getInstance().getHostname();

    private static final String ACTION_USB_PERMISSION =
            "gov.nasa.arc.irg.astrobee.picoflexx.USB_PERMISSION";

    private static final int ROYALE_VENDOR_ID = 0x1c28;
    private static final int ROYALE_PRODUCT_IDS[] = {0xC012, 0xC00F, 0xC010};

    private PendingIntent mUsbPi;
    private UsbManager mUsbManager;

    private View mBtnStart, mBtnStop;
    private TextView mLblError;
    private CheckBox mChkProfile;

    private static final int WHAT_STATE = 2;

    private Handler mUsbHandler;
    private final UIHandler mUiHandler = new UIHandler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
            case WHAT_STATE:
                setState(msg.arg1, msg.arg2);
                return true;
            default:
                return false;
            }
        }
    });

    // native methods
    public native boolean openPicoflexx(int fd);
    public native boolean startCapturing(boolean profile);
    public native boolean stopCapturing();
    public native void setExposure();
    public native int getMaxWidth();
    public native int getMaxHeight();

    // broadcast receiver for usb permission dialog
    // this can only be shown if a user is present, thus we do not need to check the
    // mUserPresent condition
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (device == null)
                return;

            switch (action) {
            case ACTION_USB_PERMISSION:
                boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                if (!granted) {
                    Log.e(TAG, "permission denied for device: " + device);
                    setState(STATE_ERROR, R.string.error_no_permission);
                    return;
                }

                performUsbPermissionCallback(device);
                break;

            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                if (!isRoyaleDevice(device)) return;

                if (mConn != null) {
                    Log.i(TAG, "Another picoflexx attached");
                    return;
                }

                if (mUsbManager.hasPermission(device)) {
                    performUsbPermissionCallback(device);
                    return;
                }

                mUsbManager.requestPermission(device, mUsbPi);
                break;

            case UsbManager.ACTION_USB_ACCESSORY_DETACHED:
                if (!isRoyaleDevice(device))
                    return;

                Log.i(TAG, "did the device disappear beneath us?");
                break;
            }
        }
    };

    public MainActivity() {
        super("Picoflexx ROS Node", "Picoflexx", ROS_MASTER_URI);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnStart = findViewById(R.id.main_btn_start);
        mBtnStop = findViewById(R.id.main_btn_stop);
        mChkProfile = (CheckBox) findViewById(R.id.main_chk_profile);
        mLblError = (TextView) findViewById(R.id.main_lbl_error);

        mUsbPi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        HandlerThread handlerThread = new HandlerThread("picoflexx[usb]");
        handlerThread.start();
        mUsbHandler = new Handler(handlerThread.getLooper());

        mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (mUsbManager == null) {
            Log.wtf(TAG, "UsbManager invalid, wtf");
            setState(STATE_ERROR, R.string.error_android);
            return;
        }

        Intent i = getIntent();
        if (i.hasExtra(Constants.EXTRA_MASTER_URI)) {
            try {
                ROS_MASTER_URI = new URI(i.getStringExtra(Constants.EXTRA_MASTER_URI));
            } catch (URISyntaxException e) {
                Log.e(TAG, "ROS Master URI invalid");
                finish();
            }
        }

        if (i.hasExtra(Constants.EXTRA_HOSTNAME)) {
            ROS_HOSTNAME = i.getStringExtra(Constants.EXTRA_HOSTNAME);
        }

        // TODO(tfmorse): support picoflexx serial number filtering via intent extra
        setState(STATE_START);
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, intentFilter);

        mUsbHandler.post(new Runnable() {
            @Override
            public void run() {
                openCamera();
            }
        });

        // TODO(tfmorse): turning on screen after starting causes buttons to be wrong
    }

    @Override
    protected void onStop() {
        unregisterReceiver(mUsbReceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mUsbHandler.getLooper().quitSafely();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: " + intent.toString());
        if (!intent.hasExtra(Constants.EXTRA_ACTION))
            return;

        final String action = intent.getStringExtra(Constants.EXTRA_ACTION);

        Intent original = getIntent();
        original.putExtra(Constants.EXTRA_ACTION, action);
        setIntent(original);

        switch (action) {
        case Constants.ACTION_START:
            Log.i(TAG, "Starting capture");
            onStartCaptureClicked(mBtnStart);
            break;
        case Constants.ACTION_STOP:
            Log.i(TAG, "Stopping capture");
            onStopCaptureClicked(mBtnStop);
            break;
        case Constants.ACTION_DIE:
            Log.i(TAG, "DIE");
            break;
        default:
            Log.e(TAG, "Unknown action: " + action);
        }
     }

    private static boolean isRoyaleDevice(final UsbDevice device) {
        final int vid = device.getVendorId();
        final int pid = device.getProductId();

        if (vid != ROYALE_VENDOR_ID) {
            return false;
        }

        for (int id : ROYALE_PRODUCT_IDS) {
            if (id == pid)
                return true;
        }
        return false;
    }

    public void onStartCaptureClicked(View v) {
        if (mConn == null) {
            Log.w(TAG, "onCaptureClicked: no camera connection");
            return;
        }

        if (!startCapturing(mChkProfile.isChecked())) {
            setState(STATE_ERROR, R.string.error_start);
            return;
        }

        setState(STATE_CAPTURING, -1);
    }

    public void onStopCaptureClicked(View v) {
        stopCapturing();
        setState(STATE_IDLE);
    }

    public void openCamera() {
        Log.d(TAG, "openCamera");

        mUiHandler.obtainMessage(WHAT_STATE, STATE_DISCOVERING, -1).sendToTarget();

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Log.d(TAG, String.format("%d USB devices", deviceList.size()));

        boolean found = false;
        for (UsbDevice device : deviceList.values()) {
            // TODO(tfmorse): deal with multiple picoflexx devices
            if (isRoyaleDevice(device)) {
                Log.d(TAG, "royale device found");
                found = true;
                if (!mUsbManager.hasPermission(device)) {
                    mUsbManager.requestPermission(device, mUsbPi);
                } else {
                    performUsbPermissionCallback(device);
                }
                break;
            }
        }

        if (!found) {
            Log.e(TAG, "No royale device found.");
            mUiHandler.obtainMessage(WHAT_STATE, STATE_NO_CAMERA, R.string.error_no_camera)
                    .sendToTarget();
        }
    }

    UsbDeviceConnection mConn = null;
    UsbDevice mPicoflexx = null;

    private void performUsbPermissionCallback(final UsbDevice device) {
        if (mUsbHandler.getLooper().getThread() != Thread.currentThread()) {
            mUsbHandler.post(new Runnable() {
                @Override
                public void run() {
                    performUsbPermissionCallback(device);
                }
            });
            return;
        }

        if (mPicoflexx != null) {
            Log.d(TAG, "Already have a picoflexx");
            return;
        }

        UsbDeviceConnection conn = mUsbManager.openDevice(device);
        Log.i(TAG, "USB Device: " + device.getDeviceName() + ", fd: " + conn.getFileDescriptor());

        if (!openPicoflexx(conn.getFileDescriptor())) {
            Log.e(TAG, "error initializing the picoflexx");
            mUiHandler.obtainMessage(WHAT_STATE, STATE_ERROR, R.string.error_initializing)
                    .sendToTarget();
            conn.close();
            return;
        }

        mConn = conn;
        mPicoflexx = device;

        final int width = getMaxWidth();
        final int height = getMaxHeight();

        mCloud.setWidth(width);
        mCloud.setHeight(height);
        mCloud.setRowStep(width * 12);

        mUiHandler.obtainMessage(WHAT_STATE, STATE_IDLE, -1).sendToTarget();
    }

    sensor_msgs.PointCloud2 mCloud = null;
    final AtomicInteger mSeqNum = new AtomicInteger(1);
    ChannelBuffer mChannelBuffer = null;
    boolean mExposureSet = false;

    @SuppressWarnings("unused")
    protected final void onNewData(final ByteBuffer data) {
        if (!mExposureSet) {
            mUiHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mExposureSet)
                        return;
                    setExposure();
                    mExposureSet = true;
                }
            });
        }

        if (mPublisher == null)
            return;
        if (!mPublisher.hasSubscribers())
            return;

        data.order(ByteOrder.nativeOrder());
        data.rewind();

        if (mChannelBuffer == null) {
            mChannelBuffer = ChannelBuffers.wrappedBuffer(data);
        }

        mChannelBuffer.setIndex(0, mChannelBuffer.capacity());

        mCloud.getHeader().setStamp(mConnectedNode.getCurrentTime());
        mCloud.getHeader().setSeq(mSeqNum.incrementAndGet());
        mCloud.setData(mChannelBuffer);

        mPublisher.publish(mCloud);
    }

    private Publisher<sensor_msgs.PointCloud2> mPublisher = null;
    private ConnectedNode mConnectedNode;
    private ScheduledFuture<?> mShutdownFuture = null;

    @Override
    protected void init(final NodeMainExecutor node) {
        NodeConfiguration nodeConfiguration =
                NodeConfiguration.newPublic(
                        ROS_HOSTNAME, ROS_MASTER_URI);

        final PointCloudNode pcn = new PointCloudNode();
        node.execute(pcn, nodeConfiguration);
        mShutdownFuture = node.getScheduledExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                node.shutdownNodeMain(pcn);
                MainActivity.this.nodeMainExecutorService.forceShutdown();
            }
        }, 3, TimeUnit.SECONDS);
    }

    class PointCloudNode extends AbstractNodeMain {
        private static final String TAG = "PointCloudNode";

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of("perch_cam");
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            Log.i(TAG, "onStart: starting...");

            if (mShutdownFuture != null) {
                mShutdownFuture.cancel(false);
            }

            Publisher<sensor_msgs.PointCloud2> publisher =
                    connectedNode.newPublisher(getDefaultNodeName().join("image"),
                            sensor_msgs.PointCloud2._TYPE);
            publisher.setLatchMode(false);
            mPublisher = publisher;
            mConnectedNode = connectedNode;

            // Pre-fill a message
            mCloud = publisher.newMessage();
            mCloud.setIsDense(false);
            mCloud.setIsBigendian(ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN);
            mCloud.getHeader().setFrameId("perch_image_link");

            MessageFactory mf = connectedNode.getTopicMessageFactory();

            // Fill in the fields
            int offset = 0;
            for (final String name : Arrays.asList("x", "y", "z")){
                final PointField pf = mf.newFromType(PointField._TYPE);

                pf.setName(name);
                pf.setCount(1);
                pf.setOffset(offset);
                pf.setDatatype(PointField.FLOAT32);

                mCloud.getFields().add(pf);
                offset += 4;
            }

            mCloud.setPointStep(offset);
        }
    }

    private int mCurrentState = 0;
    private static final int STATE_START = 1;
    private static final int STATE_NO_CAMERA = 2;
    private static final int STATE_DISCOVERING = 3;
    private static final int STATE_IDLE = 4;
    private static final int STATE_CAPTURING = 5;
    private static final int STATE_ERROR = 6;

    void setState(final int state) {
        setState(state, -1);
    }

    void setState(final int state, final int strRes) {
        mLblError.setVisibility(View.GONE);
        mBtnStart.setEnabled(false);
        mBtnStop.setEnabled(false);

        switch (state) {
        case STATE_START:
        case STATE_NO_CAMERA:
        case STATE_DISCOVERING:
            break;
        case STATE_IDLE:
            mBtnStart.setEnabled(true);
            break;
        case STATE_CAPTURING:
            mBtnStop.setEnabled(true);
            break;
        case STATE_ERROR:
            break;
        }

        if (strRes != -1) {
            mLblError.setText(strRes);
            mLblError.setVisibility(View.VISIBLE);
        }

        mCurrentState = state;
    }
}
