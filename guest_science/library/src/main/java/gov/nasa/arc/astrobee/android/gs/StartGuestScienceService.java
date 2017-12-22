package gov.nasa.arc.astrobee.android.gs;

import android.app.Service;
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

/**
 * Created by kmbrowne on 11/21/17.
 */

public abstract class StartGuestScienceService extends Service {
    private boolean mBound;
    private Messenger mService = null;
    private String mFullApkName = "";

    private static final String SERVICE_PACKAGE_NAME =
            "gov.nasa.arc.astrobee.android.gs.manager";
    private static final String SERVICE_CLASSNAME =
            "gov.nasa.arc.astrobee.android.gs.manager.MessengerService";
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mService = new Messenger(iBinder);
            mBound = true;
            // Only start guest science if send messenger succeeded
            if (sendMessenger()) {
                onGuestScienceStart();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
        }
    };

    class IncomingCommandHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MessageType.CMD.toInt()) {
                Bundle data = msg.getData();
                if (data != null) {
                    if (data.containsKey("command")) {
                        onGuestScienceCustomCmd(msg.getData().getString("command"));
                    } else {
                        Log.e("GuestScienceLib", "Command not found in message of type command! " +
                                "This shouldn't happen. If it does, contact the Astrobee guest " +
                                "science team.");
                    }
                } else {
                    Log.e("GuestScienceLib", "Command message didn't contain data! This shouldn't" +
                            " happen. If it does, contact the Astrobee guest science team.");
                }
            } else if (msg.what == MessageType.STOP.toInt()) {
                onGuestScienceStop();
            } else {
                Log.e("GuestScienceLib", "Message type not recognized! This shouldn't happen. If " +
                        "it does, contact the Astrobee guest science team.");
            }
        }
    }

    final Messenger mCommandMessenger = new Messenger(new IncomingCommandHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mFullApkName = getApplicationContext().getPackageName();

        Intent bindIntent = new Intent();
        bindIntent.setClassName(SERVICE_PACKAGE_NAME, SERVICE_CLASSNAME);
        // Bind to the guest science manager service
        bindService(bindIntent, mConnection, Context.BIND_AUTO_CREATE);

        return START_NOT_STICKY;
    }

    public boolean sendMessenger() {
        // Send messenger to guest science manager so that the guest science manager can
        // communicate with the guest science apk
        Message msg = Message.obtain(null, MessageType.MESSENGER.toInt());
        Bundle dataBundle = new Bundle();
        dataBundle.putString("apkFullName", mFullApkName);
        dataBundle.putParcelable("commandMessenger", mCommandMessenger);
        msg.setData(dataBundle);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e("GuestScienceLib", e.getMessage(), e);
            return false;
        }
        return true;
    }

    public abstract void onGuestScienceCustomCmd(String command);

    public abstract void onGuestScienceStart();

    public abstract void onGuestScienceStop();

    @Override
    public void onDestroy() {
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        super.onDestroy();
    }

    public void terminate() {
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    public void sendStarted(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Started\"}");
    }

    public void sendStopped(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Stopped\"}");
    }

    public void sendReceivedCustomCommand(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Received Custom Command\"}");
    }

    public void sendData(MessageType type, String topic, String data) {
        byte[] byteData = data.getBytes();
        sendMsg(type, topic, byteData);
    }

    public void sendData(MessageType type, String topic, byte[] data) {
        sendMsg(type, topic, data);
    }

    public void sendMsg(MessageType type, String topic, byte[] data) {
        if (!mBound) {
            Log.e("GuestScienceLib", "Not bound to guest science manager. This shouldn't happen. " +
                    "If it does, contact the Astrobee guest science team.,");
            return;
        }

        if (data.length > 2048) {
            throw new RuntimeException("Data passed to sendData function is too big to send to " +
                    "ground. Must be 2K.");
        }

        Bundle dataBundle = new Bundle();
        dataBundle.putString("apkFullName", mFullApkName);
        dataBundle.putString("topic", topic);
        dataBundle.putByteArray("data", data);

        Message msg = Message.obtain(null, type.toInt());
        msg.setData(dataBundle);
        try {
            mService.send(msg);
        } catch (RemoteException e) {
            Log.e("GuestScienceLib", e.getMessage(), e);
        }
    }
}