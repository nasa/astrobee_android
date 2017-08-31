

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

package gov.nasa.arc.irg.astrobee.rgbled;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.ros.android.RosActivity;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import ff_msgs.CommandArg;

public class MainActivity extends RosActivity {
    private static final URI sMasterUri = URI.create("http://10.42.0.100:11311");

    private static final int BLINK_VENDOR_ID = 10168;
    private static final int BLINK_PRODUCT_ID = 493;

    private static final String ACTION_USB_PERMISSION = "gov.nasa.arc.irg.astrobee.rgbled.USB_PERMISSION";

    private UsbDevice mBlinkDevice;
    private UsbDeviceConnection mBlinkConn;
    private UsbManager mUsbManager;
    private PendingIntent mPermIntent;

    private class SeekText implements SeekBar.OnSeekBarChangeListener {
        final private SeekBar mSeekBar;
        final private TextView mTextView;

        SeekText(int seekId, int textId, int defaultValue) {
            mSeekBar = (SeekBar) MainActivity.this.findViewById(seekId);
            mTextView = (TextView) MainActivity.this.findViewById(textId);

            mSeekBar.setOnSeekBarChangeListener(this);
            mSeekBar.setProgress(defaultValue);
        }

        public void setValue(int value) {
            mSeekBar.setProgress(value);
        }

        public int getValue() {
            return mSeekBar.getProgress();
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mTextView.setText(Integer.toString(progress));
            if (fromUser) {
                onColorChanged();
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) { }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            Log.i("SeekText", "Stopped tracking Touch");
        }
    }

    void onColorChanged() {
        final int red = mRed.getValue();
        final int green = mGreen.getValue();
        final int blue = mBlue.getValue();

        mImageView.setBackgroundColor(Color.argb(255, red, green, blue));
        if (mBlinkDevice != null) {
            mUsbHandler.post(new Runnable() {
                @Override
                public void run() {
                    byte[] bytes = {0x01, 0x63, (byte) (red & 0xFF), (byte) (green & 0xFF), (byte) (blue & 0xFF), 0, 0, 0, 0};
                    mBlinkConn.controlTransfer(
                            UsbConstants.USB_TYPE_CLASS | UsbConstants.USB_DIR_OUT,
                            0x09, 1, 0, bytes, 9, 0);
                }
            });
        }

        // Publish new color over ROS
        synchronized (mPublisherLock) {
            if (mColorPublisher == null)
                return;

            std_msgs.ColorRGBA msg = mColorPublisher.newMessage();
            msg.setA(255.0f);
            msg.setR(red);
            msg.setG(green);
            msg.setB(blue);
            mColorPublisher.publish(msg);
        }
    }

    public MainActivity() {
        super("RGB LED Test", "ROS RGB LED OMGWTFBBQNASADNSIPATM", sMasterUri);
    }

    private SeekText mRed;
    private SeekText mGreen;
    private SeekText mBlue;
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRed = new SeekText(R.id.seek_red, R.id.txt_red, 255);
        mGreen = new SeekText(R.id.seek_green, R.id.txt_green, 255);
        mBlue = new SeekText(R.id.seek_blue, R.id.txt_blue, 255);
        mImageView = (ImageView) findViewById(R.id.img_preview);
        mImageView.setBackgroundColor(Color.argb(0, 255, 255, 255));

        mUsbManager = (UsbManager) getSystemService(USB_SERVICE);
        mPermIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);

        mUsbThread = new HandlerThread("USB Handler");
        mUsbThread.start();
        mUsbHandler = new Handler(mUsbThread.getLooper());
        mMainHandler = new Handler();
    }

    @Override
    protected void onDestroy() {
        mUsbThread.getLooper().quitSafely();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(ACTION_USB_PERMISSION);
        registerReceiver(mUsbReceiver, intentFilter);

        enumerateDevices();
    }

    protected void enumerateDevices() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            if (device.getVendorId() == BLINK_VENDOR_ID && device.getProductId() == BLINK_PRODUCT_ID) {
                if(!mUsbManager.hasPermission(device)) {
                    mUsbManager.requestPermission(device, mPermIntent);
                } else {
                    setupUsb(device);
                }
            }
        }
    }

    protected void setupUsb(UsbDevice device) {
        UsbInterface inf = device.getInterface(0);
        UsbDeviceConnection conn = mUsbManager.openDevice(device);
        if (conn == null) {
            Log.wtf("MainActivity", "unable to open device?");
            return;
        }

        if (!conn.claimInterface(inf, true)) {
            conn.close();
            Log.wtf("MainActivity", "unable to claim interface!");
            return;
        }

        mBlinkDevice = device;
        mBlinkConn = conn;
    }

    protected void teardownUsb() {
        if (mBlinkDevice == null) {
            return;
        }

        mBlinkConn.releaseInterface(mBlinkDevice.getInterface(0));
        mBlinkConn.close();

        mBlinkDevice = null;
        mBlinkConn = null;
    }

    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("USB Receiver", "Received intent");
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device == null) {
                    Log.wtf("USB Receiver", "device is null?");
                    return;
                }

                Log.i("USB Receiver", "Device attached: " + device.getVendorId() + ":" + device.getProductId());

                if (device.getVendorId() == BLINK_VENDOR_ID && device.getProductId() == BLINK_PRODUCT_ID) {
                    if(!mUsbManager.hasPermission(device)) {
                       mUsbManager.requestPermission(device, mPermIntent);
                    } else {
                        setupUsb(device);
                    }
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                Log.i("USB Receiver", "Device detached :(");
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    if (device.getVendorId() == BLINK_VENDOR_ID && device.getProductId() == BLINK_PRODUCT_ID) {
                        teardownUsb();
                    }
                }
            } else if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        Log.i("UsbReceiver", "Got permission for device");
                        setupUsb(device);
                    }
                }
                else {
                    Log.d("UsbReceiver", "permission denied for device " + device);
                }
            }
        }
    };

    @Override
    protected void onStop() {
        teardownUsb();
        unregisterReceiver(mUsbReceiver);

        super.onStop();
    }

    private HandlerThread mUsbThread;
    private Handler mUsbHandler;
    private Handler mMainHandler;

    @Override
    protected void init(NodeMainExecutor node) {
        RGBLEDNode listener = new RGBLEDNode();

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());

        node.execute(listener, nodeConfiguration);
    }

    final Object mPublisherLock = new Object();
    Publisher<std_msgs.ColorRGBA> mColorPublisher = null;

    class RGBLEDNode extends AbstractNodeMain {

        @Override
        public GraphName getDefaultNodeName() {
            return GraphName.of("rgb_led");
        }

        @Override
        public void onStart(ConnectedNode connectedNode) {
            Subscriber<ff_msgs.CommandStamped> subscriber = connectedNode.newSubscriber(
                    getDefaultNodeName().join("command"), ff_msgs.CommandStamped._TYPE);
            subscriber.addMessageListener(new MessageListener<ff_msgs.CommandStamped>() {
                private static final String TAG = "Subscriber<Command>";

                @Override
                public void onNewMessage(final ff_msgs.CommandStamped cmd) {
                    Log.i(TAG, "I received a command");

                    if (!cmd.getCmdName().equals("set_color")) {
                        Log.e(TAG, "wrong command name");
                        return;
                    }

                    final List<ff_msgs.CommandArg> args = cmd.getArgs();
                    if (args.size() != 3) {
                        Log.e(TAG, "wrong number of args");
                        return;
                    }

                    final ff_msgs.CommandArg red = args.get(0);
                    final ff_msgs.CommandArg green = args.get(1);
                    final ff_msgs.CommandArg blue = args.get(2);

                    if (red.getDataType() != CommandArg.DATA_TYPE_INT ||
                            blue.getDataType() != CommandArg.DATA_TYPE_INT ||
                            green.getDataType() != CommandArg.DATA_TYPE_INT) {
                        Log.e(TAG, "args not integers");
                        return;
                    }

                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mRed.setValue(red.getI());
                            mGreen.setValue(green.getI());
                            mBlue.setValue(blue.getI());
                            onColorChanged();
                        }
                    });
                }
            });

            Publisher<std_msgs.ColorRGBA> publisher = connectedNode.newPublisher(
                    getDefaultNodeName().join("color"),
                    std_msgs.ColorRGBA._TYPE);
            publisher.setLatchMode(true);

            synchronized(mPublisherLock) {
                mColorPublisher = publisher;
            }
        }

        @Override
        public void onShutdown(Node node) {
            synchronized (mPublisherLock) {
                mColorPublisher = null;
            }

            super.onShutdown(node);
        }
    }
}
