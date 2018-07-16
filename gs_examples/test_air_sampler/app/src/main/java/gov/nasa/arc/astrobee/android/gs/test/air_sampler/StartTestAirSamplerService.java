package gov.nasa.arc.astrobee.android.gs.test.air_sampler;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Created by kmbrowne on 10/27/17.
 */

public class StartTestAirSamplerService extends StartGuestScienceService {
    private String mLatestCommand;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        mLatestCommand = command;
        String msg;
        sendReceivedCustomCommand("info");
        try {
            JSONObject obj = new JSONObject(command);
            String commandStr = obj.getString("name");
            Log.i("onGuestScienceCustomCmd", "Air sampler received command " + commandStr + ".");
            if (commandStr.contentEquals("on")) {
                msg = "{\"Summary\": \"Air Sampler was turned on!\"}";
                sendData(MessageType.JSON, "info", msg);
            } else if (commandStr.contentEquals("off")) {
                msg = "{\"Summary\": \"Air Sampler was turned off!\", \"Run Time\": \"40 minutes\"}";
                sendData(MessageType.JSON, "info", msg);
            } else if (commandStr.contentEquals("take")) {
                msg = "{\"Summary\": \"sample 1: 26.81, sample 2: 24.96, sample 3: 27.35\"}";
                int size_to_add = 2048 - msg.length();
                Log.i("onGuestScienceCustomCmd", "Size to add is: " + Integer.toString(size_to_add));
                for (int i = 0; i < size_to_add; i++) {
                    msg = msg.concat(Integer.toString(i%10));
                }
                Log.i("onGuestScienceCustomCmd", "Sample msg: " + msg);
                sendData(MessageType.JSON, "data", msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuestScienceStart() {
        sendStarted("info");
        sendData(MessageType.JSON, "version", "{\"Version\": \"1.0\"}");
        String path = getGuestScienceDataBasePath();
        String json_path = "{\"GS Data Base Path\": \"" + path + "\"}";
        sendData(MessageType.JSON, "info", json_path);
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        terminate();
    }
}
