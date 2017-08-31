
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

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.CameraName;
import gov.nasa.arc.astrobee.types.CameraResolution;
import gov.nasa.arc.astrobee.types.DownloadMethod;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
import gov.nasa.arc.astrobee.types.PoweredComponent;
import gov.nasa.arc.astrobee.types.TelemetryType;
import gov.nasa.arc.astrobee.Robot;

public interface BaseRobot {

    /**
     * Grab control of an agent
     *
     * Do not use this command. Guest science apks don't have to grab control
     * of the robot. Only ground operators need to use this command.
     *
     * @param cookie
     * @return PendingResult of this command
     */
    PendingResult grabControl(String cookie);

    /**
     * Request a string that can be used to grab control of an agent
     *
     * Do not use this command. Guest science apks don't have to request/grab
     * control of the robot. Only ground operators need to use this command.
     *
     * @return PendingResult of this command
     */
    PendingResult requestControl();

    /**
     * @return PendingResult of this command
     */
    PendingResult fault();

    /**
     * Command used to load a nodelet in the system. Doesn't work with nodes
     * running on the HLP.
     *
     * @param nodeletName
     * @param type Type of nodelet (namespace/classname). The type is specified
     *             in the system monitor config file so you may not need to
     *             specify this.
     * @param managerName Name of nodelet manager. This should be unnecessary
     *                    since the system monitor should have received a
     *                    heartbeat at startup from the node and the nodelet
     *                    manager name is in the heartbeat. The system monitor
     *                    saves it and should be able to use it to load
     *                    nodelets. If commands fails, you may want to try to
     *                    specify it.
     * @param bondId Can be left blank.
     * @return PendingResult of this command
     */
    PendingResult loadNodelet(String nodeletName,
                              String type,
                              String managerName,
                              String bondId);

    /**
     * @return PendingResult of this command
     */
    PendingResult noOp();

    /**
     * @return PendingResult of this command
     */
    PendingResult reacquirePosition();

    /**
     * Put free flyer in hibernate power mode.
     *
     * @return PendingResult of this command
     */
    PendingResult shutdown();

    /**
     * Command used to unload a nodelet in the system. Doesn't work with nodes
     * running on the HLP. With great power comes great responsibility! Don't
     * unload a nodelet crucial to the system!!
     *
     * @param nodeletName
     * @param managerName This should be unnecessary since the system monitor
     *                    should have received a heartbeat at startup from the
     *                    node and the heartbeat contains thenodelet manager
     *                    name. If the command fails, you may want to try to
     *                    specify it.
     * @return PendingResult of this command
     */
    PendingResult unloadNodelet(String nodeletName, String managerName);

    /**
     * This command wakes astrobee from a hibernated state.
     *
     * Do not use this command. It is a dock command, not an astrobee command.
     *
     * @param berthNumber
     * @return PendingResult of this command
     */
    PendingResult wake(int berthNumber);

    /**
     * Erases everything on the hlp.
     *
     * Do not use this command. It will erase your running apk.
     *
     * @return PendingResult of this command
     */
    PendingResult wipeHlp();

    /**
     * Move arm while perched to control camera angle
     *
     * @param pan
     * @param tilt
     * @param which Whether to perform a pan, tilt, or both.
     * @return PendingResult of this command
     */
    PendingResult armPanAndTilt(float pan, float tilt, ActionType which);

    /**
     * Open or close gripper
     *
     * @param open
     * @return PendingResult of this command
     */
    PendingResult gripperControl(boolean open);

    /**
     * @return PendingResult of this command
     */
    PendingResult stopArm();

    /**
     * @return PendingResult of this command
     */
    PendingResult stowArm();

    /**
     * Clear data
     *
     * @param dataMethod
     * @return PendingResult of this command
     */
    PendingResult clearData(DownloadMethod dataMethod);

    /**
     * Start downloading data
     *
     * Data can only be downloaded when docked.
     *
     * @param dataMethod
     * @return PendingResult of this command
     */
    PendingResult downloadData(DownloadMethod dataMethod);

    /**
     * Set active data-to-disk configuration to be the data-to-disk file most
     * recently uplinked; the file specifies which data to save to free flyer
     * onboard storage, and at what rates
     *
     * @return PendingResult of this command
     */
    PendingResult setDataToDisk();

    /**
     * Stop downloading data
     *
     * @param dataMethod
     * @return PendingResult of this command
     */
    PendingResult stopDownload(DownloadMethod dataMethod);

    /**
     * Pass data to guest science APK
     *
     * @param apkName Specify which guest science APK to send the data to
     * @param command The data to send (e.g. could be JSON-encoded data
     *                structure)
     * @return PendingResult of this command
     */
    PendingResult customGuestScience(String apkName, String command);

    /**
     * Start guest science APK
     *
     * @param apkName Specify which guest science APK to start
     * @return PendingResult of this command
     */
    PendingResult startGuestScience(String apkName);

    /**
     * Terminate guest science APK
     *
     * @param apkName Specify which guest science APK to terminate
     * @return PendingResult of this command
     */
    PendingResult stopGuestScience(String apkName);

    /**
     * @return PendingResult of this command
     */
    PendingResult autoReturn();

    /**
     * Dock Astrobee. Must meet dock approach preconditions (positioned at dock
     * approach point, etc).
     *
     * @param berthNumber Berth number can only be 1 or 2.
     * @return PendingResult of this command
     */
    PendingResult dock(int berthNumber);

    /**
     * Stop propulsion impeller motors
     *
     * @return PendingResult of this command
     */
    PendingResult idlePropulsion();

    /**
     * @return PendingResult of this command
     */
    PendingResult perch();

    /**
     * Astrobee teleop move command
     *
     * Do not use this method. Instead, use {@link Robot#simpleMove6DOF(Point,
     * Quaternion)}.
     *
     * @param referenceFrame which reference frame to use
     * @param xyz target point
     * @param xyzTolerance Not used! Tolerance is dictated by the flight mode.
     * @param rot target attitude
     * @return PendingResult of this command
     */
    PendingResult simpleMove6DOF(String referenceFrame,
                                 Point xyz,
                                 Vec3d xyzTolerance,
                                 Quaternion rot);

    /**
     * Stop teleop motion. Stop plan execution and pause plan.
     *
     * @return PendingResult of this command
     */
    PendingResult stopAllMotion();

    /**
     * Undock Astrobee
     *
     * @return PendingResult of this command
     */
    PendingResult undock();

    /**
     * @return PendingResult of this command
     */
    PendingResult unperch();

    /**
     * Pause the running plan
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult pausePlan();

    /**
     * Run the loaded plan
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult runPlan();

    /**
     * Set active plan to be the plan file that was most recently uploaded
     *
     * @return PendingResult of this command
     */
    PendingResult setPlan();

    /**
     * Skip next trajectory or command in the plan
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult skipPlanStep();

    /**
     * Pause plan for specified duration. Do nothing if docked/perched,
     * otherwise station keep.
     *
     * @param duration seconds to pause
     * @return PendingResult of this command
     */
    PendingResult wait(float duration);

    /**
     * Power off an item within Astrobee
     *
     * @param which Any component within Astrobee that can be turned on or off.
     * @return PendingResult of this command
     */
    PendingResult powerOffItem(PoweredComponent which);

    /**
     * Power on an item within Astrobee
     *
     * @param which Any component within Astrobee that can be turned on or off.
     * @return PendingResult of this command
     */
    PendingResult powerOnItem(PoweredComponent which);

    /**
     * Generic command used to make up a command after the Control Station
     * freeze.
     *
     * @param commandName
     * @param param
     * @return PendingResult of this command
     */
    PendingResult genericCommand(String commandName, String param);

    /**
     * Set camera parameters.
     *
     * @param cameraName Camera name
     * @param resolution Desired frame size in pixels.
     * @param frameRate Applies to both modes of camera.
     * @param bandwidth Only for sci camera; related to quality, may change
     *                  name to bitrate.
     * @return PendingResult of this command
     */
    PendingResult setCamera(CameraName cameraName,
                            CameraResolution resolution,
                            float frameRate,
                            float bandwidth);

    /**
     * Set camera to record video.
     *
     * @param cameraName Camera name
     * @param record Record camera video.
     * @return PendingResult of this command
     */
    PendingResult setCameraRecording(CameraName cameraName, boolean record);

    /**
     * Set streaming camera video to the ground.
     *
     * @param cameraName Camera name
     * @param stream Send live video to the ground.
     * @return PendingResult of this command
     */
    PendingResult setCameraStreaming(CameraName cameraName, boolean stream);

    /**
     * Command to turn on and off the obstacle detector
     *
     * @param checkObstacles
     * @return PendingResult of this command
     */
    PendingResult setCheckObstacles(boolean checkObstacles);

    /**
     * Command to turn on and off checking keepout zones
     *
     * @param checkZones
     * @return PendingResult of this command
     */
    PendingResult setCheckZones(boolean checkZones);

    /**
     * Command to allow auto return
     *
     * @param enableAutoReturn
     * @return PendingResult of this command
     */
    PendingResult setEnableAutoReturn(boolean enableAutoReturn);

    /**
     * @param which Specify which flashlight.
     * @param brightness Brightness percentage between 0 - 1
     * @return PendingResult of this command
     */
    PendingResult setFlashlightBrightness(FlashlightLocation which,
                                          float brightness);

    /**
     * Command to allow blind flying
     *
     * @param enableHolonomic
     * @return PendingResult of this command
     */
    PendingResult setHolonomicMode(boolean enableHolonomic);

    /**
     * Set mass and inertia matrix for Astrobee control
     *
     * @param name
     * @param mass
     * @param matrix The moment of inertia tensor. Must be a symmetric matrix.
     * @return PendingResult of this command
     */
    PendingResult setInertia(String name, float mass, Mat33f matrix);

    /**
     * Change the value of Astrobee operating limits
     *
     * @param profileName
     * @param flightMode Defines GN&C gains, hard limits, tolerances, etc.
     * @param targetLinearVelocity The maximum linear velocity to target while
     *                             translating
     * @param targetLinearAcceleration The maximum linear acceleration to
     *                                 target while translating
     * @param targetAngularVelocity The maximum angular velocity to target
     *                              while rotating
     * @param targetAngularAcceleration The maximum angular acceleration to
     *                                  target while rotating
     * @param collisionDistance Minimum distance margin to maintain away from
     *                          obstacles
     * @return PendingResult of this command
     */
    PendingResult setOperatingLimits(String profileName,
                                     FlightMode flightMode,
                                     float targetLinearVelocity,
                                     float targetLinearAcceleration,
                                     float targetAngularVelocity,
                                     float targetAngularAcceleration,
                                     float collisionDistance);

    /**
     * Change the frequency at which one type of telemetry is sent to GDS
     *
     * @param name
     * @param rate
     * @return PendingResult of this command
     */
    PendingResult setTelemetryRate(TelemetryType name, float rate);

    /**
     * Set active keepout zones to be the zones file that was most recently
     * uploaded
     *
     * @return PendingResult of this command
     */
    PendingResult setZones();

}
