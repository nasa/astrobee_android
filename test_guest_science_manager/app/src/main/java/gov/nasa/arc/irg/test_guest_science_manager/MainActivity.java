package gov.nasa.arc.irg.test_guest_science_manager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Messenger;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.StandaloneActionMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    private TextView mFullName;

    private final static String info_action = "gov.nasa.arc.astrobee.android.gs.INFO_INQUIRY";
    private final static String log_tag = "MainActivity";

    private Map<String, PendingIntent> intents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFullName = (TextView) findViewById(R.id.tv_apk_full_name);

        intents = new HashMap<>();
    }

    public void onClickPushMe(View v) {
        Intent info_intent = new Intent(info_action);
        info_intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        sendOrderedBroadcast(info_intent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle results = getResultExtras(true);
                Set<String> keys = results.keySet();
                Object[] keys_array = keys.toArray();
                Log.i(log_tag, "Size of keys is " + keys.size());
                for (int i = 0; i < keys.size(); i++) {
                    String full_name = keys_array[i].toString();
                    Bundle apk_info = results.getBundle(full_name);
                    if (!apk_info.containsKey("shortName") ||
                            !apk_info.containsKey("primary") ||
                            !apk_info.containsKey("startIntent")) {
                        continue;
                    }

                    String short_name = apk_info.getString("shortName");
                    boolean primary = apk_info.getBoolean("primary");
                    PendingIntent start_intent = apk_info.getParcelable("startIntent");
                    if (start_intent == null) {
                        Log.e(log_tag, "Start intent is null during receive broadcast.");
                    }
                    ArrayList<String> commands = apk_info.getStringArrayList("commands");
                    Log.i(log_tag, "Key[" + i + "]: " + full_name);
                    Log.i(log_tag, "Short name: " + short_name);
                    Log.i(log_tag, "Primary: " + primary);
                    for (int j = 0; j < commands.size(); j++) {
                        String command_name = commands.get(j);
                        if (!apk_info.containsKey(command_name)) {
                            continue;
                        }
                        String command_syntax = apk_info.getString(command_name);
                        if (command_syntax != null) {
                            Log.i(log_tag, "Command name: " + command_name + " command syntax: " + command_syntax);
                        }
                    }
                    Log.i("broadcast", "apk name is " + full_name);
                    intents.put(full_name, start_intent);
                }
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    public void onClickGetRunningServices(View v) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            Log.e("MainActivity", service.service.getClassName());
        }
    }

    public void onClickStartTestApk(View v) {
        Log.i("onClickStartTestApk", "Yay I was clicked.");
        mFullName.setText("Start Test Apk button pushed!");
        if (intents.containsKey("gov.nasa.arc.irg.test_guest_science_apk")) {
            PendingIntent start_apk_intent = intents.get("gov.nasa.arc.irg.test_guest_science_apk");
            try {
                start_apk_intent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MainActivity", "Couldn't start test gs apk since we don't have a pending intent.");
        }
    }

    public void onClickStopTestApk(View v) {
        Log.i("onClickStopTestApk", "Yay I was clicked.");
        mFullName.setText("Stop Test Apk button pushed!");
        MessengerService service = MessengerService.getSingleton();
        service.sendGuestScienceStop("gov.nasa.arc.irg.test_guest_science_apk");
    }

    public void onClickTestApkNoOp(View v) {
        Log.i("onClickTestApkNoOp", "Yay I was clicked.");
        mFullName.setText("Send Test Apk No Op Command button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_guest_science_apk";
        String command = "{\"name\": \"noOp\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickTestApkShowImage(View v) {
        Log.i("onClickTestApkShowImage", "Yay I was clicked.");
        mFullName.setText("Send Test Apk Send Image Command button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_guest_science_apk";
        String command = "{\"name\": \"showImage\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickTestApkStoreImage(View v) {
        Log.i("onClickTestApkStoreImg", "Yay I was clicked.");
        mFullName.setText("Send Test Apk Store Image Command button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_guest_science_apk";
        String command = "{\"name\": \"storeImage\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickTestApkResizeImage(View v) {
        Log.i("onClickTestApkResizeImg", "Yay I was clicked.");
        mFullName.setText("Send Test Apk Resize Image Command button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_guest_science_apk";
        String command = "{\"name\": \"resizeImage\", \"width\": 512, \"height\": 256}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickStartRfidReader(View v) {
        Log.i("onClickStartRfidReader", "Yay I was clicked.");
        mFullName.setText("Start RFID Reader button pushed!");
        if (intents.containsKey("gov.nasa.arc.irg.test_rfid_reader")) {
            PendingIntent start_apk_intent = intents.get("gov.nasa.arc.irg.test_rfid_reader");
            try {
                start_apk_intent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MainActivity", "Couldn't start rfid apk since we don't have the pending intent.");
        }
    }

    public void onClickStopRfidReader(View v) {
        Log.i("onClickStopRfidReader", "Yay I was clicked.");
        mFullName.setText("Stop RFID Reader button pushed!");
        MessengerService service = MessengerService.getSingleton();
        service.sendGuestScienceStop("gov.nasa.arc.irg.test_rfid_reader");
    }

    public void onClickRfidReaderNoOp(View v) {
        Log.i("onClickRfidReaderNoOp", "Yay I was clicked.");
        mFullName.setText("Send RFID Reader No Op button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_rfid_reader";
        String command = "{\"name\": \"noOp\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickRfidReaderSendInventory(View v) {
        Log.i("onClickRfidSndInventory", "Yay I was clicked.");
        mFullName.setText("Send RFID Reader Send Inventory button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_rfid_reader";
        String command = "{\"name\": \"send\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickRfidReaderRemoveItem(View v) {
        Log.i("onClickRfidRemoveItem", "Yay I was clicked.");
        mFullName.setText("Send RFID Reader Remove Item button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_rfid_reader";
        String command = "{\"name\": \"remove\", \"item\": \"Science Camera\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickStartAirSampler(View v) {
        Log.i("onClickStartAirSampler", "Yay I was clicked.");
        mFullName.setText("Start Air Sampler button pushed!");
        if (intents.containsKey("gov.nasa.arc.irg.test_air_sampler")) {
            PendingIntent start_apk_intent = intents.get("gov.nasa.arc.irg.test_air_sampler");
            try {
                start_apk_intent.send();
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MainActivity", "Couldn't start air sampler apk since we don't have the pending intent.");
        }
    }

    public void onClickStopAirSampler(View v) {
        Log.i("onClickStopAirSampler", "Yay I was clicked.");
        mFullName.setText("Stop Air Sampler button pushed!");
        MessengerService service = MessengerService.getSingleton();
        service.sendGuestScienceStop("gov.nasa.arc.irg.test_air_sampler");
    }

    public void onClickAirSamplerNoOp(View v) {
        Log.i("onClickAirSamplerNoOp", "Yay I was clicked.");
        mFullName.setText("Send Air Sampler No Op button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_air_sampler";
        String command = "{\"name\": \"noOp\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickAirSamplerTurnOn(View v) {
        Log.i("onClickAirSamplerTurnOn", "Yay I was clicked.");
        mFullName.setText("Send Air Sampler Turn On button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_air_sampler";
        String command = "{\"name\": \"on\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickAirSamplerTurnOff(View v) {
        Log.i("onClickAirSmplerTurnOff", "Yay I was clicked.");
        mFullName.setText("Send Air Sampler Turn Off button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_air_sampler";
        String command = "{\"name\": \"off\"}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }

    public void onClickAirSamplerTakeReadings(View v) {
        Log.i("onClickAirSTakeReadings", "Yay I was clicked.");
        mFullName.setText("Send Air Sampler Take Readings button pushed!");
        MessengerService service = MessengerService.getSingleton();
        String apk_full_name = "gov.nasa.arc.irg.test_air_sampler";
        String command = "{\"name\": \"take\", \"num\": 3, \"time_between\": 10}";
        service.sendGuestScienceCustomCommand(apk_full_name, command);
    }
}
