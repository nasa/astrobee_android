
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

package gov.nasa.arc.irg.astrobee.test_gs_api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class StartTestGsApiService extends StartGuestScienceService {

    // The API implementation
    ApiCommandImplementation api = null;

    private final int REQUEST_CARDINAL_MOVEMENT = 0;
    private final int REQUEST_POINT_MOVEMENT = 1;
    private final int REQUEST_POSITION = 2;
    private final int REQUEST_ARM_ACTION = 3;

    /**
     * This function is called when the GS manager sends a custom command to your apk.
     * Please handle your commands in this function.
     *
     * @param command
     */
    @Override
    public void onGuestScienceCustomCmd(String command) {
        sendReceivedCustomCommand("info");

        try {

            JSONObject jsonCommand = new JSONObject(command);
            JSONObject jResponse = new JSONObject();

            String commandStr = jsonCommand.getString("name");
            double meters = jsonCommand.has("meters") ? Double.parseDouble(jsonCommand.getString("meters")) : 0;

            Result result;

            Log.i("onGuestScienceCustomCmd", "GS_API received command " + commandStr + ".");

            switch (commandStr) {
                case "moveT":
                    Point goalPosition = new Point(jsonCommand.getDouble("x"), jsonCommand.getDouble("y"), jsonCommand.getDouble("z"));
                    //api.setPlanner(PlannerType.QP);

                    result = api.moveTo(goalPosition, new Quaternion());
                    jResponse = buildJSONResponse(result, REQUEST_POINT_MOVEMENT);
                    break;
                case "moveX":
                    result = api.relativeMoveInAxis(ApiCommandImplementation.X_AXIS, meters, new Quaternion());
                    jResponse = buildJSONResponse(result, REQUEST_CARDINAL_MOVEMENT);
                    break;
                case "moveY":
                    result = api.relativeMoveInAxis(ApiCommandImplementation.Y_AXIS, meters, new Quaternion());
                    jResponse = buildJSONResponse(result, REQUEST_CARDINAL_MOVEMENT);
                    break;
                case "moveZ":
                    result = api.relativeMoveInAxis(ApiCommandImplementation.Z_AXIS, meters, new Quaternion());
                    jResponse = buildJSONResponse(result, REQUEST_CARDINAL_MOVEMENT);
                    break;
                case "dock":
                    result = api.dock();
                    jResponse = buildJSONResponse(result, REQUEST_POINT_MOVEMENT);
                    break;
                case "undock":
                    result = api.undock();
                    jResponse = buildJSONResponse(result, REQUEST_POINT_MOVEMENT);
                    break;
                case "getPosition":
                    Kinematics k = api.getTrustedRobotKinematics(5);
                    Point currentPosition = null;
                    if (k != null) {
                        currentPosition = k.getPosition();
                    }
                    jResponse = buildJSONResponse(currentPosition, REQUEST_POSITION);
                    break;
                case "deployArm":
                    PendingResult pending = api.getRobot().armPanAndTilt(0f, ApiCommandImplementation.ARM_TILT_DEPLOYED_VALUE, ActionType.TILT);
                    result = api.getCommandResult(pending, false, -1);
                    jResponse = buildJSONResponse(result, REQUEST_ARM_ACTION);
                    break;
                case "stowArm":
                    pending = api.getRobot().armPanAndTilt(0f, ApiCommandImplementation.ARM_TILT_STOWED_VALUE, ActionType.TILT);
                    result = api.getCommandResult(pending, false, -1);
                    jResponse = buildJSONResponse(result, REQUEST_ARM_ACTION);
                    break;
                default:
                    jResponse.put("Summary", "Unknown Command");
                    break;
            }

            sendData(MessageType.JSON, "data", jResponse.toString());

        } catch (JSONException e) {
            sendData(MessageType.JSON, "data", "{\"Summary\": \"Error parsing JSON:(\"}");
        }
    }

    /**
     * This function is called when the GS manager starts your apk. Put all of your start up code in here.
     */
    @Override
    public void onGuestScienceStart() {
        api = ApiCommandImplementation.getInstance();
        Log.i("TEST", "Hey, i passed getInstance");
        sendStarted("info");
    }

    /**
     * This function is called when the GS manager stops your apk.
     * Put all of your clean up code in here. You should also call the terminate helper function
     * at the very end of this function.
     */
    @Override
    public void onGuestScienceStop() {
        //api.shutdownFactory();
        sendStopped("info");
        terminate();
    }

    private JSONObject buildJSONResponse(Result commandResult, int requestType) throws JSONException {
        if (commandResult == null) {
            return new JSONObject().put("Summary", "ERROR : Command was NOT executed. Internal error and/or timeout");
        }

        if (requestType == REQUEST_POSITION) {
            return new JSONObject().put("Summary", "Invalid request type for command result");
        }

        return buildJSONResponse((Object)commandResult, requestType);
    }

    private JSONObject buildJSONResponse(Point commandResult, int requestType) throws JSONException {
        if (commandResult == null) {
            return new JSONObject().put("Summary", "Unable to get a trusted position within the timeout");
        }

        if (requestType != REQUEST_POSITION) {
            return new JSONObject().put("Summary", "Invalid request type for command result");
        }

        return buildJSONResponse((Object)commandResult, requestType);
    }


    private JSONObject buildJSONResponse(Object commandResult, int requestType) throws JSONException {

        Result result = commandResult instanceof Result ? (Result) commandResult : null;
        Point position = commandResult instanceof Point ? (Point) commandResult : null;

        JSONObject jResponse = new JSONObject();

        // TODO (rgarciar) Improve this switch
        switch (requestType) {
            case REQUEST_POSITION:
                jResponse.put(
                        "Summary",
                        new JSONObject().put("Position", position.toString())
                );
                break;
            case REQUEST_ARM_ACTION:
                if (!result.hasSucceeded()) {
                    jResponse.put(
                            "Summary",
                            new JSONObject()
                                    .put("Command Status", result.getStatus().toString())
                                    .put("Message", result.getMessage())
                    );
                } else {
                    jResponse.put(
                            "Summary",
                            new JSONObject()
                                    .put("Command Status", result.getStatus().toString())
                                    .put("Message", "DONE!!")
                    );
                }
                break;
            case REQUEST_CARDINAL_MOVEMENT:
            case REQUEST_POINT_MOVEMENT:
                if (!result.hasSucceeded()) {
                    jResponse.put(
                            "Summary",
                            new JSONObject()
                                    .put("Command Status", result.getStatus().toString())
                                    .put("Message", result.getMessage())
                    );
                } else {
                    jResponse.put(
                            "Summary",
                            new JSONObject()
                                    .put("Command Status", result.getStatus().toString())
                                    .put("Message", "DONE!!")
                                    .put(
                                            "Data",
                                            new JSONObject().put("Position", api.getTrustedRobotKinematics(5).getPosition())
                                    )
                    );
                }
                break;
            default:
                jResponse.put("Summary", "ERROR: Unknown command type");
                break;
        }
        return jResponse;
    }
}
