
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

package gov.nasa.arc.irg.astrobee.test_simple_trajectory;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class StartTestTrajectoryService extends StartGuestScienceService {

    private final String LOCATION_USLAB = "us_lab";
    private final String LOCATION_JEM = "jem";
    private final String LOCATION_GRANITE = "granite";

    // Fixed trajectory points for simulation (US LAB)
    private final Point POINT_1_USLAB = new Point(1, 0.1, 4.8);
    private final Point POINT_2_USLAB = new Point(4, 0.1, 4.8);
    private final Point POINT_3_USLAB = new Point(4, -0.1, 4.8);
    private final Point POINT_4_USLAB = new Point(1, -0.1, 4.8);

    // Fixed trajectory points for simulation (JEM)
    private final Point POINT_1_JEM = new Point(10.7, -9.2, 4.5);
    private final Point POINT_2_JEM = new Point(11.4, -9.5, 4.5);
    private final Point POINT_3_JEM = new Point(11.4, -8.5, 4.5);
    private final Point POINT_4_JEM = new Point(10.7, -8.5, 4.5);

    //Fixed trajectory points for granite laboratory or granite simulation.
    private final Point POINT_1_GRANITE = new Point(-0.1, 0.5, -0.7);
    private final Point POINT_2_GRANITE = new Point(0.3, 0.5, -0.7);
    private final Point POINT_3_GRANITE = new Point(0.3, 0.0, -0.7);
    private final Point POINT_4_GRANITE = new Point(-0.1, 0.15, -0.7);

    // Orientations used in trajectories
    private final Quaternion ORIENT_0_0_0 = new Quaternion(0, 0, 0, 1);
    private final Quaternion ORIENT_0_0_N90 = new Quaternion(0, 0, -0.707f, 0.707f);
    private final Quaternion ORIENT_0_0_90 = new Quaternion(0, 0, 0.707f, 0.707f);
    private final Quaternion ORIENT_0_0_N180 = new Quaternion(0, 0, 1, 0);

    // The API implementation
    private ApiCommandImplementation api = null;

    // Default location. Change this depending on testing location
    private String default_location = LOCATION_JEM;

    private Point[] arrayPoint = null;
    private Quaternion[] arrayOrient = null;

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
            // Transform the String command into a JSON object so we can read it.
            JSONObject jCommand = new JSONObject(command);

            // Get the name of the command we received. See commands.xml files in res folder.
            String sCommand = jCommand.getString("name");

            // JSON object that will contain the data we will send back to the GSM and GDS
            JSONObject jResult = new JSONObject();

            // This variable will contain the result of the last successful or unsuccessful movement
            Result result;

            switch (sCommand) {
                case "doTrajectory":
                    // Execute trajectory
                    if (default_location == LOCATION_JEM) {
                        result = doUndockMoveDock();
                    } else {
                        result = doTrajectory();
                    }
                    break;
                case "getPath":
                    String path = getGuestScienceDataBasePath();
                    sendData(MessageType.JSON, "data", new JSONObject()
                            .put("Summary", new JSONObject()
                                    .put("GS Path", path)).toString());
                    return;
                default:
                    // Inform GS Manager and GDS, then stop execution.
                    sendData(MessageType.JSON, "data", "ERROR: Unrecognized command");
                    return;
            }

            if (result == null) {
                // There were no points to loop
                jResult.put("Summary", new JSONObject()
                        .put("Status", "ERROR")
                        .put("Message", "Trajectory was not defined"));
            } else if (!result.hasSucceeded()) {
                // If a goal point failed.
                jResult.put("Summary", new JSONObject()
                        .put("Status", result.getStatus())
                        .put("Message", result.getMessage()));
            } else {
                // Success!
                jResult.put("Summary", new JSONObject()
                        .put("Status", result.getStatus())
                        .put("Message", "DONE!"));
            }

            // Send data to the GS manager to be shown on the Ground Data System.
            sendData(MessageType.JSON, "data", jResult.toString());

        } catch (JSONException e) {
            // Send an error message to the GSM and GDS
            sendData(MessageType.JSON, "data", "ERROR parsing JSON");
        } catch (Exception ex) {
            // Send an error message to the GSM and GDS
            sendData(MessageType.JSON, "data", "Unrecognized ERROR");
        }
    }

    /**
     * This function is called when the GS manager starts your apk.
     * Put all of your start up code in here.
     */
    @Override
    public void onGuestScienceStart() {
        // Set trajectory points and orientations
        switch (default_location) {
            case LOCATION_GRANITE:
                arrayPoint = new Point[]{
                        POINT_1_GRANITE, POINT_2_GRANITE, POINT_3_GRANITE, POINT_4_GRANITE,
                        POINT_4_GRANITE};
                arrayOrient = new Quaternion[]{
                        ORIENT_0_0_0, ORIENT_0_0_0, ORIENT_0_0_N180, ORIENT_0_0_N90, ORIENT_0_0_0};
                break;
            case LOCATION_JEM:
                arrayPoint = new Point[]{
                        POINT_2_JEM, POINT_3_JEM, POINT_4_JEM, POINT_1_JEM};
                arrayOrient = new Quaternion[]{
                        ORIENT_0_0_0, ORIENT_0_0_90, ORIENT_0_0_N180, ORIENT_0_0_0};
                break;
            case LOCATION_USLAB:
                arrayPoint = new Point[]{
                        POINT_1_USLAB, POINT_2_USLAB, POINT_3_USLAB, POINT_4_USLAB, POINT_1_USLAB};
                arrayOrient = new Quaternion[]{
                        ORIENT_0_0_0, ORIENT_0_0_0, ORIENT_0_0_N90, ORIENT_0_0_N180, ORIENT_0_0_0};
                break;
        }

        // Get a unique instance of the Astrobee API in order to command the robot.
        api = ApiCommandImplementation.getInstance();

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
        // Stop the API
        api.shutdownFactory();

        // Inform the GS manager and the GDS that this app stopped.
        sendStopped("info");

        // Destroy all connection with the GS Manager.
        terminate();
    }

    /**
     * Undock Astrobee, perform a trajectory (previously defined) and dock again.
     *
     * @return
     */
    private Result doUndockMoveDock() {
        // Undock
        Result result = api.undock();
        if (!result.hasSucceeded()) {
            return result;
        }

        // Do trajectory
        result = doTrajectory();
        if (result == null || !result.hasSucceeded()) {
            // There were no points to loop or one goal failed
            return result;
        }

        // TODO Dock -- COMING SOON
        //result = api.dock();
        return result;
    }

    /**
     * Execute a simple trajectory from the previously specified points and orientations.
     *
     * @return A Result from the execution of the move command in the Astrobee API.
     */
    private Result doTrajectory() {

        Result result = null;

        // Loop the points and orientation previously defined.
        for (int i = 0; i < arrayPoint.length; i++) {
            result = api.moveTo(arrayPoint[i], arrayOrient[i]);
            api.stopAllMotion();
            if (!result.hasSucceeded()) {
                // If any movement fails we cancel all execution.
                break;
            }
        }

        return result;
    }
}
