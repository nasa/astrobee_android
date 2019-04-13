// Copyright 2017 Intelligent Robotics Group, NASA ARC

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
import gov.nasa.arc.astrobee.types.LocalizationMode;
import gov.nasa.arc.astrobee.types.PlannerType;
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
     * Initialize bias.
     *
     * @return PendingResult of this command
     */
    PendingResult initializeBias();

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
     * Reset ekf.
     *
     * @return PendingResult of this command
     */
    PendingResult resetEkf();

    /**
     * Put free flyer in hibernate power mode.
     *
     * @return PendingResult of this command
     */
    PendingResult shutdown();

    /**
     * This command is used to switch between localization pipelines.
     *
     * @param mode Specify which pipeline to switch to.
     * @return PendingResult of this command
     */
    PendingResult switchLocalization(LocalizationMode mode);

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
     * This command is used to unterminate the robot. It will only reset the
     * terminate flag but will not start up the pmcs or repower the payloads.
     *
     * @return PendingResult of this command
     */
    PendingResult unterminate();

    /**
     * This command wakes astrobee from a hibernated state into a nominal
     * state.
     *
     * Do not use this command. It is a dock command, not an astrobee command.
     *
     * @param berthNumber
     * @return PendingResult of this command
     */
    PendingResult wake(int berthNumber);

    /**
     * This command wakes astrobee from a hibernated state into a safe state.
     *
     * Do not use this command. It is a dock command, not an astrobee command.
     *
     * @param berthNumber
     * @return PendingResult of this command
     */
    PendingResult wakeSafe(int berthNumber);

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
     * It takes the prop modules a couple of seconds to ramp up to the
     * requested flight mode if not already at that flight mode. This command
     * ramps up the prop modules so that when a move command is issued, it will
     * execute right away. This doesn't need to be used for nominal use but may
     * be used/needed for astrobee synchronization.
     *
     * @return PendingResult of this command
     */
    PendingResult prepare();

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
     * This command is only used for segments in a plan. Currently GDS does not
     * handle timestamps since it is so tricky to get them right. So plan
     * timestamps start at 0 and are not used by mobility since the Astrobee's
     * system time is not 0. Thus in the nominal case set enable immediate
     * should be set to true so that every segment sent to the mobility
     * subsystem will be started immediately. If someone wants to do
     * synchronized Astrobee movement, they can create plans by hand and have
     * the timestamps start in the future. They will also have to set enable
     * immediate to false so that the mobility system abides by the timestamp.
     * Please note that these plans need to be started close to the first
     * timestamp. If they are not, you could end up waiting a long time for the
     * robot to start moving. Also the timestamp cannot be in the past or the
     * segment will be skipped. It is probably easier to juse use GDS plan. To
     * do this, you will want to upload the plans to the robots, use the
     * prepare command to get the robots ready to move, and then run the plans.
     * There could be up to a half of a second delay between the robots
     * starting their plans. See the set time sync command. Astrobee to
     * Astrobee communication is in the works and may solve synchronizing
     * Astrobees in a better way.
     *
     * @param enableImmediate
     * @return PendingResult of this command
     */
    PendingResult setEnableImmediate(boolean enableImmediate);

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
     * @param centerOfMass The center of mass of Astrobee.
     * @param matrix The moment of inertia tensor. Must be a symmetric matrix.
     * @return PendingResult of this command
     */
    PendingResult setInertia(String name,
                             float mass,
                             Vec3d centerOfMass,
                             Mat33f matrix);

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
     * This command is used to switch planners.
     *
     * @param planner Specify which planner to switch to.
     * @return PendingResult of this command
     */
    PendingResult setPlanner(PlannerType planner);

    /**
     * Change the frequency at which one type of telemetry is sent to GDS
     *
     * @param name
     * @param rate
     * @return PendingResult of this command
     */
    PendingResult setTelemetryRate(TelemetryType name, float rate);

    /**
     * This command is used to help with Astrobee synchronization. It will try
     * to account for the delay in communication between the ground and space
     * and the time it takes to plan and validate a segment. This will
     * hopefully result in two Astrobees starting to move at the same time.
     *
     * @param setTimeSync
     * @return PendingResult of this command
     */
    PendingResult setTimeSync(boolean setTimeSync);

    /**
     * Set active keepout zones to be the zones file that was most recently
     * uploaded
     *
     * @return PendingResult of this command
     */
    PendingResult setZones();

}
