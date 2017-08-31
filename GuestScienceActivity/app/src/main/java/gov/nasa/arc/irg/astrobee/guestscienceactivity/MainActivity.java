
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
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;

public class MainActivity extends Activity implements View.OnClickListener, ServiceConnection{

    private static final String LOGTAG = "MainGS_Activity";
    private static final int  MSG_REGISTER_CLIENT = 1;
    private static final int MSG_UNREGISTER_CLIENT = 2;
    private static final int MSG_SET_INT_VALUE = 3;
    private static final int MSG_SET_STRING_VALUE = 4;
    private static final String GUESTSCIENCE_SERVICE_CONTEXT =
            "gov.nasa.arc.irg.astrobee.messengerservice";
    private static final String GUESTSCIENCE_SERVICE_CLASSNAME =
            "gov.nasa.arc.irg.astrobee.messengerservice.MyMessengerService";

    private Button btnBind, btnUnbid;
    private TextView textCommStatus;
    private TextSwitcher textSentCmd, textResponse;

    private Messenger mServiceMessenger = null;
    private final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
    private ServiceConnection mServiceConnection = this;
    boolean mIsBound;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnBind = (Button) findViewById(R.id.btnBind);
        btnUnbid = (Button) findViewById(R.id.btnUnbind);
        textCommStatus = (TextView) findViewById(R.id.text_commStatus);
        textSentCmd = (TextSwitcher) findViewById(R.id.text_sentMessage);
        textResponse = (TextSwitcher) findViewById(R.id.text_receiveMessage);

        btnBind.setOnClickListener(this);
        btnUnbid.setOnClickListener(this);

    }

    private void doBindService() {
        Intent intent = new Intent();
        intent.setClassName(GUESTSCIENCE_SERVICE_CONTEXT, GUESTSCIENCE_SERVICE_CLASSNAME);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        textCommStatus.setText("Comm Status: Binded");
    }

    private void doUnbindService() {
        if(mIsBound){
            if (mServiceMessenger != null) {
                try {
                    Message msg = Message.obtain(null, MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mServiceMessenger.send(msg);
                } catch (RemoteException re) {

                }
            }

        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {

    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    public void onClick(View v) {

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

            switch (msg.what){
                case MSG_SET_STRING_VALUE:
                    String str1 = msg.getData().getString("str1");
                    textResponse.setText(str1);
                    break;
                default:
                    super.handleMessage(msg);
            }

        }
    }
}
