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

package gov.nasa.arc.irg.astrobee.battery_monitor;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import gov.nasa.arc.irg.astrobee.battery_monitor.types.Battery;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */
public class StartBatteryService extends StartGuestScienceService{
    /**
     * This function is called when the GS manager sends a custom command to your apk.
     * Please handle your commands in this function.
     *
     * @param command
     */
    @Override
    public void onGuestScienceCustomCmd(String command) {
        /* Inform the Guest Science Manager (GSM) and the Ground Data System (GDS)
         * that this app received a command. */
        sendReceivedCustomCommand("info");

        try {
            JSONObject obj = new JSONObject(command);
            String commandStr = obj.getString("name");

            JSONObject jResponse = new JSONObject();

            switch (commandStr) {
                case "backUI":
                    // TODO Find a way to hide the activity without showing the launcher
                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                    jResponse.put("Summary", "Interface on background");
                    break;
                case "showUI":
                    Intent startActivity = new Intent(this, MainActivity.class);
                    startActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startActivity);
                    jResponse.put("Summary", "Interface on foreground");
                    break;
                case "getData":
                    String batteryName = obj.getString("battery");
                    BatteryStatusNode node = BatteryStatusNode.getInstance();
                    Battery battery = null;

                    switch (batteryName) {
                        case "top_left":
                            battery = node.batteryTopLeft;
                            break;
                        case "top_right":
                            battery = node.batteryTopRight;
                            break;
                        case "bottom_left":
                            battery = node.batteryBottomLeft;
                            break;
                        case "bottom_right":
                            battery = node.batteryBottomRight;
                            break;
                        default:
                            break;
                    }

                    jResponse.put("Summary", battery.toJSON());

                    break;
                case "getAllData":
                    node = BatteryStatusNode.getInstance();
                    Battery[] batteries = node.getBatteries();
                    JSONArray jBatteriesArray = new JSONArray();

                    for(Battery b : batteries) {
                        jBatteriesArray.put(b.toJSON());
                    }

                    JSONObject jBattObject = new JSONObject();
                    jBattObject.put("Batteries", jBatteriesArray);

                    jResponse.put("Summary", jBattObject);

                    break;

                default:
                    jResponse.put("Summary", "ERROR: Command not found");
                    break;
            }

            sendData(MessageType.JSON, "data", jResponse.toString());

        } catch (JSONException e) {
            sendData(MessageType.JSON, "data", "{\"Summary\": \"Error parsing JSON:(\"}");
            e.printStackTrace();
        }
    }

    /**
     * This function is called when the GS manager starts your apk.
     * Put all of your start up code in here.
     */
    @Override
    public void onGuestScienceStart() {
        // Start the interface
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        // Inform the GS Manager and the GDS that the app has been started.
        sendStarted("info");
    }

    /**
     * This function is called when the GS manager stops your apk.
     * Put all of your clean up code in here. You should also call the terminate helper function
     * at the very end of this function.
     */
    @Override
    public void onGuestScienceStop() {

        // Inform the GS manager and the GDS that this app stopped.
        sendStopped("info");

        // Destroy all connection with the GS Manager.
        terminate();
    }

}
