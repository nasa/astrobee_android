package gov.nasa.arc.astrobee.android.gs.manager;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import java.io.File;
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
                if (sendGuestScienceDataBasePath(apkFullName)) {
                    ManagerNode.INSTANCE().ackGuestScienceStart(true, apkFullName, "");
                } else {
                    String err_msg = "Either wasn't able to find sdcard folder on HLP or unable " +
                            "create gs data folders! Won't start apk without path or folders!";
                    ManagerNode.INSTANCE().ackGuestScienceStart(false, apkFullName, err_msg);
                }
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

    public boolean sendGuestScienceDataBasePath(String apkName) {
        String gs_data_base_path = "";
        if (mApkMessengers.containsKey(apkName)) {
            File sdcard_path = new File("/sdcard/");
            if (!sdcard_path.exists() || !sdcard_path.isDirectory()) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Couldn't find sdcard folder " +
                        "in file system! Major error! Unable to start GS apk without base path!");
            } else {
                gs_data_base_path = sdcard_path.getPath() + "/data/" + apkName;

                File immediate_path = new File(gs_data_base_path.toString() + "/immediate");
                if (!immediate_path.exists() || !immediate_path.isDirectory()) {
                    immediate_path.mkdirs();
                }

                File delayed_path = new File(gs_data_base_path.toString() + "/delayed");
                if (!delayed_path.exists() || !delayed_path.isDirectory()) {
                    delayed_path.mkdirs();
                }

                File incoming_path = new File(gs_data_base_path.toString() + "/incoming");
                if (!incoming_path.exists() || !incoming_path.isDirectory()) {
                    incoming_path.mkdirs();
                }

                // Check to make sure the files were created
                if (!immediate_path.exists() || !delayed_path.exists() || !incoming_path.exists()) {
                    ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Wasn't able to create " +
                            "guest science data folders! Unable to start GS apk without folders!");
                    return false;
                }

                Messenger messenger = mApkMessengers.get(apkName);
                Message msg = Message.obtain(null, MessageType.PATH.toInt());
                Bundle data_bundle = new Bundle();
                data_bundle.putString("path", gs_data_base_path.toString());
                msg.setData(data_bundle);
                try {
                    messenger.send(msg);
                } catch (RemoteException e) {
                    ManagerNode.INSTANCE().getLogger().error(LOG_TAG, e.getMessage(), e);
                    return false;
                }
            }
        } else {
            ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Couldn't find messenger for " +
                    apkName + ". Thus cannot send data base path.");
            gs_data_base_path = "";
        }

        if (gs_data_base_path == "") {
           return false;
        }
        return true;
    }

    public boolean sendGuestScienceCustomCommand(String apkName, String command) {
        if (mApkMessengers.containsKey(apkName)) {
            Messenger messenger = mApkMessengers.get(apkName);
            Message msg = Message.obtain(null, MessageType.CMD.toInt());

            // Check to make sure the message and messenger aren't null
            if (messenger == null) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Messenger for apk is null. Cannot send custom guest science command.");
                return false;
            }
            if (msg == null) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Command message for apk is null. Cannot send command to apk.");
                return false;
            }

            Bundle data_bundle = new Bundle();
            data_bundle.putString("command", command);
            msg.setData(data_bundle);

            // Send custom guest science command
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

            // Check to make sure the message and messenger aren't null
            if (messenger == null) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Messenger for apk is null. Cannot stop apk.");
                return false;
            }
            if (msg == null) {
                ManagerNode.INSTANCE().getLogger().error(LOG_TAG, "Stop message for apk is null. Cannot stop apk.");
                return false;
            }

            // Send stop message to apk
            try {
                messenger.send(msg);
                // Remove messenger so we don't try to use a dead object
                mApkMessengers.remove(apkName);
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
