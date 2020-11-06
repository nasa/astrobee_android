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

package gov.nasa.arc.irg.astrobee.sci_cam_image2;

import android.content.Intent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import android.util.Log;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */
public class StartSciCamImage extends StartGuestScienceService{

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
            case "takeSinglePicture":
                Intent intent1 = new Intent();
                intent1.setAction(SciCamImage2.TAKE_SINGLE_PICTURE);
                sendBroadcast(intent1);
                jResponse.put("Summary", "Command to take a single picture sent.");
                break;
            case "turnOnContinuousPictureTaking":
                Intent intent2 = new Intent();
                intent2.setAction(SciCamImage2.TURN_ON_CONTINUOUS_PICTURE_TAKING);
                sendBroadcast(intent2);
                jResponse.put("Summary", "Command to turn on continuous picture taking sent.");
                break;
            case "turnOffContinuousPictureTaking":
                Intent intent3 = new Intent();
                intent3.setAction(SciCamImage2.TURN_OFF_CONTINUOUS_PICTURE_TAKING);
                sendBroadcast(intent3);
                jResponse.put("Summary", "Command to turn off continuous picture taking sent.");
                break;
            case "turnOnSavingPicturesToDisk":
                Intent intent4 = new Intent();
                intent4.setAction(SciCamImage2.TURN_ON_SAVING_PICTURES_TO_DISK);
                sendBroadcast(intent4);
                jResponse.put("Summary", "Command to turn on saving pictures to disk sent.");
                break;
            case "turnOffSavingPicturesToDisk":
                Intent intent5 = new Intent();
                intent5.setAction(SciCamImage2.TURN_OFF_SAVING_PICTURES_TO_DISK);
                sendBroadcast(intent5);
                jResponse.put("Summary", "Command to turn off saving pictures to disk sent.");
                break;
            default:
                jResponse.put("Summary", "ERROR: Command not found.");
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

        // Start SciCamImage2
        Intent sciCamImageActivity = new Intent(this, SciCamImage2.class);
        sciCamImageActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(sciCamImageActivity);

        //if (SciCamImage2.doLog)
        Log.i(SciCamImage2.SCI_CAM_TAG, "SciCamImage2 started.");
        
        // Inform the GS Manager and the GDS that the app has been started.
        sendStarted("info");
    }

    /**
     * This function is called when the GS manager stops your apk.
     * Put all of your clean up code in here. You should also call the
     * terminate helper function at the very end of this function.
     */
    @Override
    public void onGuestScienceStop() {

        Log.i(SciCamImage2.SCI_CAM_TAG, "Stopping SciCamImage2.");

        // Ask SciCamImage2 to stop itself
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(SciCamImage2.STOP);
        sendBroadcast(intent);

        // Inform the GS manager and the GDS that this app stopped.
        sendStopped("info");
        
        // Destroy all connection with the GS Manager.
        terminate();

    }
    
}

