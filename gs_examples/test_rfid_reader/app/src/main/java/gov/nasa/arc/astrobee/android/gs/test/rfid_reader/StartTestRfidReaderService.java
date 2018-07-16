package gov.nasa.arc.astrobee.android.gs.test.rfid_reader;

import android.content.Intent;
import android.util.Log;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Created by kmbrowne on 10/26/17.
 */

public class StartTestRfidReaderService extends StartGuestScienceService {
    private String mLatestCommand;
    List<String> mInventory;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        mLatestCommand = command;
        sendReceivedCustomCommand("info");
        try {
            JSONObject obj = new JSONObject(command);
            String commandStr = obj.getString("name");
            Log.i("onGuestScienceCustomCmd", "Rfid reader received command " + commandStr + ".");
            if (commandStr.contentEquals("send")) {
                String jsonInventory = "{\"Summary\": \"";
                for (int i = 0; i < mInventory.size(); i++) {
                    jsonInventory += mInventory.get(i);
                    if ((i + 1) != mInventory.size()) {
                        jsonInventory += ", ";
                    } else {
                        jsonInventory += "\"}";
                    }
                }
                sendData(MessageType.JSON, "data", jsonInventory);
            } else if (commandStr.contentEquals("remove")) {
                String item = obj.getString("item");
                boolean found = false;
                for (int i = 0; i < mInventory.size() && !found; i++) {
                    if (mInventory.get(i).contentEquals(item)) {
                        found = true;
                        mInventory.remove(i);
                    }
                }

                if (!found) {
                    String msg = "Item " + item + " not found in inventory.";
                    sendData(MessageType.JSON, "info", msg);
                    return;
                }

                String jsonInventory = "{\"Summary\": \"";
                for (int i = 0; i < mInventory.size(); i++) {
                    jsonInventory += mInventory.get(i);
                    if ((i + 1) != mInventory.size()) {
                        jsonInventory += ", ";
                    } else {
                        jsonInventory += "\"}";
                    }
                }
                sendData(MessageType.JSON, "data", jsonInventory);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuestScienceStart() {
        sendStarted("info");
        String path = getGuestScienceDataBasePath();
        String json_path = "{\"GS Data Base Path\": \"" + path + "\"}";
        sendData(MessageType.JSON, "info", json_path);

        mInventory = new ArrayList<>();
        mInventory.add("Science Camera");
        mInventory.add("Hazard Camera");
        mInventory.add("Navigation Camera");
        mInventory.add("Dock Camera");
        mInventory.add("Perch Camera");
        mInventory.add("Laser");
        mInventory.add("Front Flashlight");
        mInventory.add("Back Flashlight");
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        terminate();
    }
}