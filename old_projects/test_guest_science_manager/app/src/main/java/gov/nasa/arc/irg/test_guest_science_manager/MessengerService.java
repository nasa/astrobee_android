package gov.nasa.arc.irg.test_guest_science_manager;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kmbrowne on 10/23/17.
 */

public class MessengerService extends Service {
    private static MessengerService singleton;

    private Map<String, Messenger> guest_science_apk_messengers_;

    @Override
    public void onCreate() {
        Log.i("onCreate", "Messenger service created!");
        guest_science_apk_messengers_ = new HashMap<>();
        singleton = this;
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("onStartCommand", "Messenger service started!");
        return START_NOT_STICKY;
    }

    class IncomingDataHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            // TODO(Katie) Check size of topic and data and log an error message if they are too big
            // TODO(Katie) Also check that name, topic, data, messenger are in data.
            // TODO(Katie) Do this when you combined this with the ros guest science manager.
            try {
                String apk_full_name = msg.getData().getString("apkFullName");
                if (msg.what == MessageType.STRING.toInt() ||
                        msg.what == MessageType.JSON.toInt()) {
                    String topic = msg.getData().getString("topic");
                    Log.i("handleMessage", "Yay we got a message from " + apk_full_name
                            + " with topic " + topic + "!");
                    byte[] data = msg.getData().getByteArray("data");
                    String data_str = new String(data, "UTF-8");
                    Toast.makeText(getApplicationContext(), data_str, Toast.LENGTH_LONG).show();
                } else if (msg.what == MessageType.BINARY.toInt()) {
                    Toast.makeText(getApplicationContext(), "Received binary data!", Toast.LENGTH_LONG).show();
                } else if (msg.what == MessageType.MESSENGER.toInt()) {
                    Log.i("handleMessage", "Yay we got a messenger message from " + apk_full_name);
                    Messenger gs_messenger = msg.getData().getParcelable("commandMessenger");
                    guest_science_apk_messengers_.put(apk_full_name, gs_messenger);
                } else {
                    Log.e("GSManager.HandleMessage", "Message type was not recognized.");
                }
            } catch (UnsupportedEncodingException e) {
                Log.e("testGSManager", e.getMessage(), e);
            }
        }
    }

    final Messenger m_data_messenger = new Messenger(new IncomingDataHandler());

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("onBind", "Yay we are binded to!");
        Toast.makeText(getApplicationContext(), "binding", Toast.LENGTH_SHORT).show();
        return m_data_messenger.getBinder();
    }

    public void sendGuestScienceCustomCommand(String apk_name, String command) {
        if (guest_science_apk_messengers_.containsKey(apk_name)) {
            Messenger gs_apk_messenger = guest_science_apk_messengers_.get(apk_name);
            Message msg = Message.obtain(null, MessageType.CMD.toInt());
            Bundle data_bundle = new Bundle();
            data_bundle.putString("command", command);
            msg.setData(data_bundle);
            try {
                gs_apk_messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MessService.sendGSCmd", "Couldn't find messenger for " + apk_name +
                    ". Thus cannot send command " + command + ".");
        }
    }

    public void sendGuestScienceStop(String apk_name) {
        if (guest_science_apk_messengers_.containsKey(apk_name)) {
            Messenger gs_apk_messenger = guest_science_apk_messengers_.get(apk_name);
            Message msg = Message.obtain(null, MessageType.STOP.toInt());
            try {
                gs_apk_messenger.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MessService.sendGSStop", "Couldn't find messenger for " + apk_name +
                    ". Thus cannot send a message to stop the apk.");
        }
    }

    public static MessengerService getSingleton() {
        return singleton;
    }
}
