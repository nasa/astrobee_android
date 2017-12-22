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
                msg = "{\"Summary\": \"Air Sampler was turned off!\"}";
                sendData(MessageType.JSON, "info", msg);
            } else if (commandStr.contentEquals("take")) {
                msg = "{\"Summary\": \"sample 1: 26.81, sample 2: 24.96, sample 3: 27.35\"}";
                sendData(MessageType.JSON, "data", msg);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuestScienceStart() {
        sendStarted("info");
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        terminate();
    }
}
