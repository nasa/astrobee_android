
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

package gov.nasa.arc.irg.astrobee.guestscienceactivity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.StringTokenizer;


public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection{

    private static final String LOGTAG = "MAIN_GS_Activity";
    private static final int  MSG_REGISTER_CLIENT = 1;
    private static final int MSG_UNREGISTER_CLIENT = 2;
    private static final int MSG_SET_CMD_VALUE = 3;
    private static final int MSG_SET_STRING_VALUE = 4;
    private static final int MSG_GS_TO_ROBOT = 5;
    private static final int MSG_SERVICE_TO_ROBOT = 6;
    private static final int MSG_SERVICE_TO_GS = 7;
    private static final int MSG_ROBOT_TO_SERVICE = 8;

    private static final String GUESTSCIENCE_SERVICE_CONTEXT =
            "gov.nasa.arc.irg.astrobee.android_ros_bridge";

    private static final String GUESTSCIENCE_SERVICE_CLASSNAME =
                    "gov.nasa.arc.irg.astrobee.android_ros_bridge.AndroidRosBridgeService";

    private Button mBtnBind, mBtnUnbid;
    private static ScrollView mScrollView_msgServiceToGS, mScrollView_msgRobotToService;
    private TextView mTextCommStatus;
    private static  TextView mTextMsgGSToRobot, mTextMsgServiceToGS, mTextMsgRobotToService, mTextMsgServiceToRobot;

    private static Messenger mServiceMessenger = null;
    private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
    private ServiceConnection mServiceConnection = this;
    private static boolean mIsBound;

    private GuestScienceSampleApp gsApp = new GuestScienceSampleApp();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnBind = (Button) findViewById(R.id.btn_BindService);
        mBtnUnbid = (Button) findViewById(R.id.btn_UnbindService);

        mTextCommStatus = (TextView) findViewById(R.id.text_commStatus);
        mTextMsgGSToRobot = (TextView) findViewById(R.id.text_msgGSToRobot);
        mTextMsgServiceToGS = (TextView) findViewById(R.id.text_msgServiceToGS);
        mTextMsgRobotToService = (TextView) findViewById(R.id.text_msgRobotToService);
        mTextMsgServiceToRobot = (TextView) findViewById(R.id.text_MsgServiceToRobot);

        mTextMsgServiceToGS.setMovementMethod(new ScrollingMovementMethod());
        mTextMsgRobotToService.setMovementMethod(new ScrollingMovementMethod());

        mScrollView_msgServiceToGS = (ScrollView) findViewById(R.id.scrollView_msgServiceToGS);
        mScrollView_msgRobotToService = (ScrollView) findViewById(R.id.scrollView_msgRobotToService);

        mBtnBind.setOnClickListener(this);
        mBtnUnbid.setOnClickListener(this);

        /*
        mBtnBind = (Button) findViewById(R.id.mBtnBind);
        mBtnUnbid = (Button) findViewById(R.id.btnUnbind);
        mTextCommStatus = (TextView) findViewById(R.id.text_commStatus);
        textSentCmd = (TextView) findViewById(R.id.text_sentMessage);
        textResponse = (TextView) findViewById(R.id.text_receiveMessage);
        textResponse.setMovementMethod(new ScrollingMovementMethod());

        mScrollView = (ScrollView) findViewById(R.id.scrollView);

        mBtnBind.setOnClickListener(this);
        mBtnUnbid.setOnClickListener(this);
        */

        Log.i(LOGTAG, "Activity started!");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("mTextCommStatus", mTextCommStatus.getText().toString());
        outState.putString("mTextMsgGSToRobot", mTextMsgGSToRobot.toString());
        outState.putString("mTextMsgServiceToGS", mTextMsgServiceToGS.toString());
        outState.putString("mTextMsgRobotToService", mTextMsgRobotToService.toString());
        outState.putString("mTextMsgServiceToRobot", mTextMsgServiceToRobot.toString());
        /*
        outState.putString("mTextCommStatus", mTextCommStatus.getText().toString());
        outState.putString("textSentCmd", textSentCmd.toString());
        outState.putString("textResponse", textResponse.toString());*/
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mTextCommStatus.setText(savedInstanceState.getString("mTextCommStatus"));
            mTextMsgGSToRobot.setText(savedInstanceState.getString("mTextMsgGSToRobot"));
            mTextMsgServiceToGS.setText(savedInstanceState.getString("mTextMsgServiceToGS"));
            mTextMsgRobotToService.setText(savedInstanceState.getString("mTextMsgRobotToService"));
            mTextMsgServiceToRobot.setText(savedInstanceState.getString("mTextMsgServiceToRobot"));
            /*mTextCommStatus.setText(savedInstanceState.getString("mTextCommStatus"));
            textSentCmd.setText(savedInstanceState.getString("textSentCmd"));
            textResponse.setText(savedInstanceState.getString("textResponse"));*/
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    // This helps keeping the scroll to the last line within the ScrollView
    private static void scrollToBottom(final ScrollView sv) {
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    /**
     * Send data to the service
     * @param bundleToSend Bundle that contains the name of the command and its args.
     */
    public static void sendMessageToService(Bundle bundleToSend) {
        if (mIsBound) {
            if (mServiceMessenger != null) {
                try {

                    Message msg = Message.obtain(null, MSG_GS_TO_ROBOT);
                    msg.setData(bundleToSend);
                    mServiceMessenger.send(msg);
                } catch (RemoteException re) {
                }
            }
        }
    }

    /**
     * Bind this Activity to Android-ROS bridge service
     */
    private void doBindService() {
        Intent intent = new Intent();
        intent.setClassName(GUESTSCIENCE_SERVICE_CONTEXT, GUESTSCIENCE_SERVICE_CLASSNAME);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        mTextCommStatus.setText("Comm Status: Binded");
        Log.i(LOGTAG, "BINDED to SERVICE!");
    }

    private void doUnbindService() {
        if(mIsBound){
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException re) {
                    // In this case the service has crashed before we could even do anything with it
                }
            }
            // Detach our existing connection.
            unbindService(mServiceConnection);
            mIsBound = false;
            mTextCommStatus.setText("Comm Status: Unbinded");
            Log.i(LOGTAG, "UNBINDED from SERVICE!");
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mServiceMessenger = new Messenger(service);
        mTextCommStatus.setText("Comm Status: Connected");
        Log.i(LOGTAG, "CONNECTED to SERVICE!");
        try {
            Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
            msg.replyTo = mMessenger;
            mServiceMessenger.send(msg);
        } catch (RemoteException re) {
            // In this case the service has crashed before we could even do anything with it
        }
        mTextMsgGSToRobot.setText(gsApp.commandList.get(0).getString("cmd"));
        gsApp.executeRobotInstructions("");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // This is called when the connection with the service has been unexpectedly
        // disconnected - process crashed.
        mServiceMessenger = null;
        mTextCommStatus.setText("Comm Status: Disconnected");
        Log.i(LOGTAG, "DISCONNECTED from SERVICE!");
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mBtnBind)) {
            doBindService();
        } else if(v.equals(mBtnUnbid)) {
            doUnbindService();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        } catch (Throwable t) {
            Log.e(LOGTAG, "Failed to unbind from service", t);
        }
    }

    private class IncomingMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {

            String msgRxd;
            switch (msg.what){
                case MSG_SET_STRING_VALUE:
                    msgRxd = msg.getData().getString("robotResponse");
                    break;
                case MSG_SERVICE_TO_ROBOT:
                    msgRxd = msg.getData().getString("robotResponse");
                    mTextMsgServiceToRobot.setText(msgRxd);
                    break;
                case MSG_SERVICE_TO_GS:
                    msgRxd = msg.getData().getString("robotResponse");
                    mTextMsgServiceToGS.append(msgRxd + "\n");
                    scrollToBottom(mScrollView_msgServiceToGS);
                    gsApp.executeRobotInstructions(msgRxd);
                    break;
                case MSG_ROBOT_TO_SERVICE:
                    msgRxd = msg.getData().getString("robotResponse");
                    mTextMsgRobotToService.append(msgRxd + "\n");
                    scrollToBottom(mScrollView_msgRobotToService);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }

    public static void setmTextMsgServiceToRobot(String msgRxd) {
        mTextMsgServiceToGS.append(msgRxd + "\n");
        scrollToBottom(mScrollView_msgServiceToGS);
    }

    public static void setmTextMsgGSToRobot(String msgRxd){
        mTextMsgGSToRobot.setText(msgRxd);
    }
}
