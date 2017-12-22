package gov.nasa.arc.astrobee.android.gs.manager;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by kmbrowne on 11/14/17.
 */

public class MessengerService extends Service {
    private static final String LOG_TAG = "GuestScienceManager";
    private static MessengerService sSingleton;

    private Map<String, Messenger> mApkMessengers;

    @Override
    public void onCreate() {
        mApkMessengers = new HashMap<>();
        sSingleton = this;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    class IncomingDataHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // Make sure the guest science message contains data
            if (msg.getData() == null) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Got guest science android " +
                        "message but there is nothing in the message! The message will not be " +
                        "processed!");
                return;
            }

            if (msg.what == MessageType.MESSENGER.toInt()) {
                // Make sure the guest science apk sent it's full name
                if (!msg.getData().containsKey("apkFullName")) {
                    ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Got message of type " +
                            "messenger but message doesn't contain the apk name. The message will "
                            + "not be processed and the apk will be marked not started!");
                    return;
                }

                String apkFullName = msg.getData().getString("apkFullName");
                // Make sure the guest science apk sent it's messenger
                if (!msg.getData().containsKey("commandMessenger")) {
                    String err_msg = "Got message of type messenger from apk " + apkFullName +
                            " but the message doesn't contain the messenger. The message will " +
                            "not be processed and the apk will be marked not started.";
                    ManagerNode.INSTANCE().getLogger().error(LOG_TAG, err_msg);
                    ManagerNode.INSTANCE().ackGuestScienceStart(false, apkFullName, err_msg);
                    return;
                }
                Messenger messenger = msg.getData().getParcelable("commandMessenger");
                mApkMessengers.put(apkFullName, messenger);
                ManagerNode.INSTANCE().ackGuestScienceStart(true, apkFullName, "");
            } else {
                ManagerNode.INSTANCE().onGuestScienceData(msg);
            }
        }
    }

    private final Messenger mDataMessenger = new Messenger(new IncomingDataHandler());

    @Override
    public IBinder onBind(Intent intent) {
        return mDataMessenger.getBinder();
    }

    public boolean sendGuestScienceCustomCommand(String apkName, String command) {
        if (mApkMessengers.containsKey(apkName)) {
            Messenger messenger = mApkMessengers.get(apkName);
            Message msg = Message.obtain(null, MessageType.CMD.toInt());
            Bundle data_bundle = new Bundle();
            data_bundle.putString("command", command);
            msg.setData(data_bundle);
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, e.getMessage(), e);
                return false;
            }
       } else {
            ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Couldn't find messenger for " +
                    apkName + ". Thus cannot send command " + command + ".");
            return false;
        }
        return true;
    }

    public boolean sendGuestScienceStop(String apkName) {
        if (mApkMessengers.containsKey(apkName)) {
            Messenger messenger = mApkMessengers.get(apkName);
            Message msg = Message.obtain(null, MessageType.STOP.toInt());
            try {
                messenger.send(msg);
            } catch (RemoteException e) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, e.getMessage(), e);
                return false;
            }
        } else {
            ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Couldn't find messenger for " +
                    apkName + ". Thus cannot send a message to stop the apk.");
            return false;
        }
        return true;
    }

    public static MessengerService getSingleton() {
        return sSingleton;
    }
}