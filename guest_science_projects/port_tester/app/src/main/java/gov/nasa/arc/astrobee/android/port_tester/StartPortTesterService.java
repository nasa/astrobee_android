
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

package gov.nasa.arc.astrobee.android.port_tester;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class StartPortTesterService extends StartGuestScienceService {

    private UsbService usbService;
    private boolean usbServiceRunning;
    private MyHandler mHandler;

    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed
     * through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<StartPortTesterService> mService;

        public MyHandler(StartPortTesterService service) {
            mService = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    checkPortTesterResponse(data);
                    break;
            }
        }

        public void checkPortTesterResponse(String data) {
            Log.i("TEST", data);
            if (data.equals("HelloBack")) {
                String msg = "Successfully Tested USB Port!";
                mService.get().sendGsData("OK", msg);
                Toast.makeText(mService.get().getApplicationContext(),
                        msg, Toast.LENGTH_LONG).show();
            }
        }
    }

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String msg = null;

            switch (Objects.requireNonNull(intent.getAction())) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    if (usbService != null) {
                        // if UsbService was correctly binded, Send data
                        String data = "Hello World\n";
                        usbService.write(data.getBytes());
                    }
                    msg = "Sent ping to Port Tester";
                    sendGsData("OK", msg);
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    msg = "USB Permission not granted";
                    sendGsData("ERROR", msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    msg = "USB device not supported";
                    sendGsData("ERROR", msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };

    /**
     * This function is called when the GS manager starts your apk.
     * Put all of your start up code in here.
     */
    @Override
    public void onGuestScienceStart() {
        mHandler = new MyHandler(this);
        usbServiceRunning = false;

        // Inform the GS Manager and the GDS that the app has been started.
        sendStarted("info");
    }

    /**
     * This function is called when the GS manager stops your apk.
     * Put all of your clean up code in here. You should also call the terminate helper function
     * at the very end of this function.
     */
    @Override
    public void onGuestScienceStop() {
        // TODO Stop handler too
        stopUsbService();

        // Inform the GS manager and the GDS that this app stopped.
        sendStopped("info");

        // Destroy all connection with the GS Manager.
        terminate();
    }

    /**
     * This function is called when the GS manager sends a custom command to your apk.
     * Please handle your commands in this function.
     *
     * @param command
     */
    @Override
    public void onGuestScienceCustomCmd(String command) {
        /* Inform the Guest Science Manager (GSM) and the Ground Data System (GDS)
         * that this app received a command. */
        sendReceivedCustomCommand("info");

        try {
            // Transform the String command into a JSON object so we can read it.
            JSONObject jCommand = new JSONObject(command);

            // Get the name of the command we received. See commands.xml files in res folder.
            String sCommand = jCommand.getString("name");

            // JSON object that will contain the data we will send back to the GSM and GDS
            JSONObject jResult = new JSONObject();

            switch (sCommand) {
                // You may handle your commands here
                case "start_listening":
                    setFilters();  // Start listening notifications from UsbService

                    // Start UsbService(if it was not started before) and Bind it
                    startService(UsbService.class, usbConnection, null);

                    jResult.put("Summary", new JSONObject()
                            .put("Status", "OK")
                            .put("Message", "Started listening"));

                    usbServiceRunning = true;

                    break;
                case "stop_listening":
                    stopUsbService();
                    jResult.put("Summary", new JSONObject()
                            .put("Status", "OK")
                            .put("Message", "Stopped listening"));
                    break;
                default:
                    // Inform GS Manager and GDS, then stop execution.
                    jResult.put("Summary", new JSONObject()
                            .put("Status", "ERROR")
                            .put("Message", "Unrecognized command"));
            }

            // Send data to the GS manager to be shown on the Ground Data System.
            sendData(MessageType.JSON, "data", jResult.toString());
        } catch (JSONException e) {
            // Send an error message to the GSM and GDS
            sendData(MessageType.JSON, "data", "ERROR parsing JSON");
        } catch (Exception ex) {
            // Send an error message to the GSM and GDS
            sendData(MessageType.JSON, "data", "Unrecognized ERROR");
        }
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopUsbService() {
        if (usbServiceRunning) {
            unregisterReceiver(mUsbReceiver);
            unbindService(usbConnection);
            usbServiceRunning = false;
        }
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    public void sendGsData(String status, String msg) {
        try {
            if (msg != null && status != null) {
                JSONObject jResult = new JSONObject();
                jResult.put("Summary", new JSONObject()
                        .put("Status", status)
                        .put("Message", msg));
                sendData(MessageType.JSON, "data", jResult.toString());
            }
        } catch (JSONException e) {
            // Send an error message to the GSM and GDS
            sendData(MessageType.JSON, "data", "ERROR parsing JSON");
        }
    }
}
