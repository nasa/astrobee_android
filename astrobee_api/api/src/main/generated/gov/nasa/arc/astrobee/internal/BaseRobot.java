// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.CameraMode;
import gov.nasa.arc.astrobee.types.CameraName;
import gov.nasa.arc.astrobee.types.CameraResolution;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
import gov.nasa.arc.astrobee.types.LocalizationMode;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.PoweredComponent;
import gov.nasa.arc.astrobee.types.TelemetryType;
import gov.nasa.arc.astrobee.Robot;

public interface BaseRobot {

    /**
     * Grabs control of an Astrobee.<p/>The Astrobee control station implements
     * the RAPID access control protocol, which includes the following
     * steps:<ol><li>Use Admin.requestCookie to get a unique
     * cookie.</li><li>Use this Admin.grabControl command to grab control.
     * Include the cookie received in step 1.</li></ol>The RAPID access control
     * protocol exists to prevent accidental contention between different
     * operators. Each robot can be under the control of at most one control
     * station at a time. The robot will reject most commands from other
     * control stations, but a few exceptions are made for commands that might
     * be urgently needed to safe the system: any control station can send the
     * Mobility.stopAllMotion and Mobility.idlePropulsion commands. A single
     * control station can grab control of multiple robots, for example, to
     * manage multi-robot guest science. Note that guest science apps running
     * onboard the robot do not need to grab control of the robot to send
     * commands; there is no mechanism to prevent contention between a guest
     * science app and a control station.
     *
     * Do not use this command. Guest science apks don't have to grab control
     * of the robot. Only ground operators need to use this command.
     *
     * @param cookie
     * @return PendingResult of this command
     */
    PendingResult grabControl(String cookie);

    /**
     * Requests a cookie that can be used to grab control of an Astrobee via
     * the AccessControl.grabControl command.
     *
     * Do not use this command. Guest science APKs don't have to request/grab
     * control of the robot. Only ground operators need to use this command.
     *
     * @return PendingResult of this command
     */
    PendingResult requestControl();

    /**
     * For internal use only. Places the robot in a fault state. This command
     * can only be sent by Astrobee's onboard executive or system monitor
     * nodes. The executive will reject the command if it comes from any other
     * source.
     *
     * @return PendingResult of this command
     */
    PendingResult fault();

    /**
     * Calibrates the bias parameters of Astrobee's IMU.<p/>Astrobee's IMU
     * biases slowly drift over time. While Astrobee is actively running its
     * localization system and maintaining a position fix, its EKF
     * automatically corrects for bias drift under the assumption that the
     * local environment is inertially fixed. If the localization system is
     * turned off or loses its fix for an extended period, this command should
     * be used to explicitly calibrate the biases prior to restarting motion.
     * It works by integrating the robot's linear acceleration and angular
     * velocity for about five seconds, and taking the resulting average values
     * as the new zero. During the calibration period, the robot must remain
     * stationary (e.g. docked or perched on a handrail, not held by an
     * astronaut).
     *
     * @return PendingResult of this command
     */
    PendingResult initializeBias();

    /**
     * This command is intended to load a ROS nodelet in the Astrobee flight
     * software. Please note, the underlying system monitor service can't load
     * nodelets that run on the HLP.
     *
     * @param nodeletName Name of nodelet
     * @param managerName Which nodelet manager should load the nodelet. If
     *                    left blank, the system monitor will default to using
     *                    the managerName specified in the last heartbeat it
     *                    received from the requested nodelet. (That should
     *                    normally work fine, assuming the nodelet sent out at
     *                    least one valid heartbeat.)
     * @param type Type of nodelet (namespace/classname). If left blank, the
     *             system monitor will default to the type specified in its
     *             config file, which should normally work fine.
     * @param bondId With the current implementation, you should leave this
     *               field blank. Specifying a non-empty string (unique id)
     *               will activate bond notifications.<p/>Background: ROS
     *               nodelets provide a 'bond' mechanism that enables the
     *               process requesting a nodelet load (in our case the system
     *               monitor) to receive automatic notification if that nodelet
     *               crashes. However, our system monitor instead relies on a
     *               custom heartbeat message to detect liveness, and it is not
     *               currently configured to do anything useful with bond
     *               notifications, even if they are activated.
     * @return PendingResult of this command
     */
    PendingResult loadNodelet(String nodeletName,
                              String managerName,
                              String type,
                              String bondId);

    /**
     * Does nothing. Returns a 'success' response if you have control of the
     * robot. This command may be useful for debugging communication or access
     * control issues.
     *
     * @return PendingResult of this command
     */
    PendingResult noOp();

    /**
     * Switches Astrobee's localization to use the general-purpose
     * pipeline.<p/>This command is equivalent to calling
     * Admin.switchLocalization with the "MappedLandmarks" pipeline and then
     * calling Admin.resetEKF. It could be useful as a way to manually recover
     * localization if Astrobee is lost because it is using the wrong
     * localization pipeline (e.g., if docking or perching approach got
     * interrupted, and targets are no longer in view).
     *
     * @return PendingResult of this command
     */
    PendingResult reacquirePosition();

    /**
     * Resets the EKF component of Astrobee's localization.<p/>This command
     * discards the EKF state and forces the localization system to acquire a
     * new fix. It should not be needed unless the localization system has
     * failure modes where it gets persistently stuck with invalid state
     * information. (Unfortunately, as of 10/2020, that is a frequent
     * occurrence.)
     *
     * @return PendingResult of this command
     */
    PendingResult resetEkf();

    /**
     * This rarely used command manually switches between localization
     * pipelines. Normally, Astrobee switches its localization pipeline
     * automatically based on what activity it is conducting (normal free
     * flight, docking, perching, etc.).<p/>Note that, although Astrobee could
     * logically integrate information from multiple pipelines at the same time
     * (e.g., NavCam sparse map features as well as DockCam AR targets), at
     * present, it only uses data from one of these pipelines at a time. IMU
     * information is always integrated, regardless of which pipeline is used.
     * While switching pipelines from A to B, updates from A stop, then there
     * may be a dropout period of a few seconds before B acquires a fix. During
     * the dropout period, localization relies solely on the IMU.
     *
     * @param mode Which pipeline to use:<ul><li>MappedLandmarks:
     *             General-purpose localization mode; Astrobee uses its NavCam
     *             to recognize landmark features on the ISS interior, as well
     *             as for visual odometry.</li><li>ARTags: During docking,
     *             Astrobee uses its DockCam to view AR tags located on the
     *             dock and obtain more robust and accurate
     *             localization.</li><li>Handrail: During perching approach,
     *             Astrobee uses its PerchCam LIDAR to recognize the geometry
     *             of a handrail in front of a wall.</li><li>Perch: Not
     *             implemented. While perched, Astrobee could use a simplified
     *             localization approach based on remembering its initial
     *             perched pose and updating its pose during pan/tilt motions
     *             using a kinematic model of the arm. </li><li>Truth: During
     *             lab testing, Astrobee can optionally subscribe to pose
     *             telemetry from an external ground-truth localization system
     *             and use it for motion control.</li></ul>
     * @return PendingResult of this command
     */
    PendingResult switchLocalization(LocalizationMode mode);

    /**
     * This command is intended to unload a ROS nodelet in Astrobee's flight
     * software. Its primary use is in the system monitor's fault response
     * table, where it is the usual response if initialization of a nodelet
     * fails. Please note, the internal service can't unload nodes running on
     * the HLP.
     *
     * @param nodeletName Name of nodelet
     * @param managerName Which nodelet manager should unload the nodelet. If
     *                    left blank, the system monitor will default to using
     *                    the managerName specified in the last heartbeat it
     *                    received from the nodelet. (That should normally work
     *                    fine, assuming the nodelet sent out at least one
     *                    valid heartbeat.)
     * @return PendingResult of this command
     */
    PendingResult unloadNodelet(String nodeletName, String managerName);

    /**
     * Clears the terminate flag on the robot. This functionality is tied to
     * Astrobee's physical terminate button and terminate indicator LED. The
     * terminate life cycle is as follows:<ul><li>Nominal ops: When Astrobee is
     * active, the amber terminate LED is turned on and the robot functions
     * normally.</li><li>Terminate: A crew member may press the terminate
     * button as a way to quickly power down the propulsion modules and
     * payloads, while leaving the main processors running and able to
     * communicate with the ground operator. This is a gentler way to intervene
     * than turning off the physical power switch. While terminated, the
     * terminate LED is also turned off.</li><li>Unterminate: The ground
     * operator can send this command to clear the terminate flag and turn the
     * terminate LED back on. This does <b>not</b> automatically restore power
     * to the hardware.</li><li>Configure power: Send Power.powerOnItem
     * commands as needed to restore power to individual hardware
     * components.</li></ul>
     *
     * @return PendingResult of this command
     */
    PendingResult unterminate();

    /**
     * Wakes an Astrobee from a hibernating state (only EPS powered) by
     * powering up the main processors. This command is intended to also
     * automatically start the flight software stack such that Astrobee is
     * ready to receive Astrobee API commands, but that feature is not
     * implemented.<p/>The Astrobee in question must be docked. Because each
     * wake command is routed through a dock berth, which Astrobee to wake is
     * specified using the berth number.<p/>See also Admin.wakeSafe. The
     * difference between Admin.wake and Admin.wakeSafe is that Admin.wake is
     * intended to automatically start the flight software stack. Since that
     * feature is not implemented, at present, the two commands are
     * equivalent.<p/>Note: For context about how to wake an Astrobee on the
     * ISS, see the procedure IRG-FFTEST207a Astrobee Quick Wakeup and
     * Checkout.
     *
     * Do not use this command. It is a dock command, not an Astrobee command.
     *
     * @param berthNumber Which berth the Astrobee is using. 1=left, 2=right.
     * @return PendingResult of this command
     */
    PendingResult wake(int berthNumber);

    /**
     * Wakes Astrobee from a hibernating state (only EPS powered) into a safe
     * state (main processors powered, available to log in for debugging, but
     * flight software stack not started).<p/>The Astrobee in question must be
     * docked. Because each wake command is routed through a dock berth, which
     * Astrobee to wake is specified using the berth number. See also
     * Admin.wake.
     *
     * Do not use this command. It is a dock command, not an Astrobee command.
     *
     * @param berthNumber Which berth the Astrobee is using. 1=left, 2=right.
     * @return PendingResult of this command
     */
    PendingResult wakeSafe(int berthNumber);

    /**
     * Moves Astrobee's arm.<p/>The arm has two joints. The tilt joint is used
     * to deploy/stow the arm and adjust the SciCam tilt angle while perched.
     * The pitch joint is used to adjust the SciCam pan angle while
     * perched.<p/>The (pan, tilt) = (0, 0) reference position is defined to
     * have the arm fully deployed and aligned with the robot's -X axis. If
     * perched on a handrail on an ISS wall, this position should nominally
     * make the SciCam camera axis point directly toward the opposite wall.
     * Increasing the tilt angle tilts the SciCam up, and increasing the pan
     * angle pans the SciCam to the right. The arm's stowed position is (pan,
     * tilt) = (0, 180).<p/>The arm joints will be moved
     * sequentially:<ol><li>Pan: If which is "Pan" or "Both", pan to the
     * specified pan angle.</li><li>Tilt: If which is "Tilt" or "Both", tilt to
     * the specified tilt angle.</li></ol><p/>Naturally, if you prefer to tilt
     * first, you can issue a tilt-only move, followed by a pan-only
     * move.<p/>Some mistakes to avoid include:<ul><li>While the arm is
     * partially within its payload bay (tilt > 90), the pan angle must be 0 to
     * avoid the arm colliding with its payload bay.</li><li>While the arm is
     * close to its fully stowed state (tilt > 160), the gripper must be closed
     * to avoid colliding with the arm's payload bay.</li><li>While perched,
     * the nominal tilt range of motion is restricted to -20 ..
     * +90.</li><li>While perched, the arm may not be able to realize its full
     * nominal range of motion without causing Astrobee's body to collide with
     * the wall. The actual range of motion depends on clutter and how far the
     * handrail projects from the wall.</li></ul>
     *
     * @param pan The target pan angle. Ignored if which is "Tilt".
     * @param tilt The target tilt angle. Ignored if which is "Pan".
     * @param which Specifies whether the arm needs to pan, tilt, or both.
     * @return PendingResult of this command
     */
    PendingResult armPanAndTilt(float pan, float tilt, ActionType which);

    /**
     * Deploys arm. The motion sequence is:<ol><li>Pan to 0</li><li>Tilt to
     * 0</li></ol><p/>See also Arm.armPanAndTilt for more discussion of the
     * arm.
     *
     * @return PendingResult of this command
     */
    PendingResult deployArm();

    /**
     * Opens or closes gripper.<p/>Astrobee's arm has a passively
     * under-actuated gripper with a single motor. Opening the gripper commands
     * the motor to pull on a pair of tendons that open the fingers. Closing
     * the gripper cuts power to the motor; the fingers are spring-loaded to
     * close. The first time the gripper opens, it calibrates the motor encoder
     * position by fully opening until the fingers contact a hard stop, then
     * relaxes slightly to its nominal open position. On later open cycles, it
     * opens directly to the nominal open position. Note that the motor
     * consumes significant power holding the gripper open, so it should be
     * closed when not actively in use. Holding it open for extended periods
     * (minutes) runs the risk of triggering a motor overtemperature fault,
     * which disables the motor.
     *
     * @param open Set to true/false to open/close gripper.
     * @return PendingResult of this command
     */
    PendingResult gripperControl(boolean open);

    /**
     * Stops arm motion by commanding it to hold its current joint positions.
     * Does not affect the gripper.
     *
     * @return PendingResult of this command
     */
    PendingResult stopArm();

    /**
     * Stows arm. The motion sequence is:<ol><li>Close gripper</li><li>Pan to
     * 0</li><li>Tilt to 180</li></ol><p/>See also Arm.armPanAndTilt for more
     * discussion of the arm.
     *
     * @return PendingResult of this command
     */
    PendingResult stowArm();

    /**
     * Sets the data-to-disk configuration, which specifies how to log ROS
     * telemetry topics to the robot's onboard storage.<p/>The configuration is
     * specified in a JSON-formatted file that contains a list of topic
     * entries. For each logged topic, one specifies:<ul><li>frequency:
     * Throttles the maximum rate at which to record messages on the topic.
     * Specify -1 to disable throttling. Note: Currently, throttling is not
     * implemented. You must specify -1.</li><li>downlinkOption: Which log to
     * store messages in, which must be "Immediate" or "Delayed". The intent is
     * that the "Immediate" log is for high-priority data, which can then be
     * prioritized for downlink after the activity. Note: Currently, you must
     * specify "Delayed" ("Immediate" downlink is currently reserved for
     * internal use by the flight software stack).</li></ul>Sample data-to-disk
     * configuration files can be found in
     * astrobee_ops/gds/ControlStationConfig.<p/>The Astrobee control station
     * implements the protocol for managing onboard telemetry
     * recording:<ol><li>Uplink a new data-to-disk file using the RAPID
     * compressed file protocol over DDS. (File uplink is not considered a
     * command, so it does not appear in this command dictionary).</li><li>Send
     * this Data.setDataToDisk command to load the uplinked file. Any errors in
     * the file will be reported at this time.</li><li>Use the
     * Data.startRecording / Data.stopRecording commands to start / stop
     * onboard telemetry logging.</li></ol>
     *
     * @return PendingResult of this command
     */
    PendingResult setDataToDisk();

    /**
     * Starts logging ROS telemetry to onboard storage. See Data.setDataToDisk.
     *
     * @param description An optional short description string to include in
     *                    the filename of the stored telemetry file (ROS bag).
     *                    Note that the filename always includes a timestamp,
     *                    which ensures uniqueness.
     * @return PendingResult of this command
     */
    PendingResult startRecording(String description);

    /**
     * Stops logging ROS telemetry to onboard storage, as initiated by
     * Data.startRecording. See also Data.setDataToDisk.
     *
     * @return PendingResult of this command
     */
    PendingResult stopRecording();

    /**
     * Routes a custom command to a guest science app (APK) running on the
     * Astrobee HLP.<p/>For the convenience of the control station operator,
     * each guest science app can define what custom commands it supports in a
     * configuration file read by the control station. See
     * astrobee_ops/gds/ControlStation/PlanEditorGuestScience.config.<p/>See
     * GuestScience.startGuestScience for more on the guest science life
     * cycle.
     *
     * @param apkName Which guest science APK to send the command to
     * @param command The command to send (usually formatted as a JSON
     *                dictionary, enabling an arbitrary number and types of
     *                parameters).
     * @return PendingResult of this command
     */
    PendingResult customGuestScience(String apkName, String command);

    /**
     * This command is the equivalent to issuing the stop guest science command
     * and then the start guest science command. Sometimes guest science APKs
     * become unresponsive and this is a quick way to try to get it work again.
     * <p/>See GuestScience.startGuestScience for more on the guest science
     * life cycle.
     *
     * @param apkName Which guest science APK to restart
     * @param wait The time in seconds the guest science manager waits in
     *             between sending the stop and start commands to the guest
     *             science apk. Different apks will need different wait times
     *             to allow a complete shutdown before starting again. A 2
     *             second wait time is recommended for simple guest science
     *             apks. For the sci_cam_image apk which has to release the
     *             science camera hardware resources, we empirically found a
     *             conservative wait time of 10 seconds worked reliably.
     * @return PendingResult of this command
     */
    PendingResult restartGuestScience(String apkName, int wait);

    /**
     * Starts a guest science app (APK) running on the Astrobee HLP. This
     * command is part of the guest science app life cycle, implemented by the
     * guest science manager node on the HLP. Steps of the life cycle
     * include:<p/><ul><li>startGuestScience: Starts a guest science app,
     * making it available to receive commands.</li><li>customGuestScience:
     * Routes a command to a running guest science
     * app.</li><li>stopGuestScience: Terminates a running guest science
     * app.</li></ul>
     *
     * @param apkName Which guest science APK to start
     * @return PendingResult of this command
     */
    PendingResult startGuestScience(String apkName);

    /**
     * Terminates a guest science app (APK) running on the Astrobee HLP. See
     * GuestScience.startGuestScience.
     *
     * @param apkName Which guest science APK to terminate
     * @return PendingResult of this command
     */
    PendingResult stopGuestScience(String apkName);

    /**
     * For expert use only. Returns Astrobee to its dock.<p/>As originally
     * envisioned, this operation could be commanded from anywhere on the ISS,
     * and Astrobee would plan a collision-free path back to the dock.
     * Auto-return could also be invoked as an automatic response to a
     * low-battery fault. To avoid contention for dock berths when multiple
     * Astrobees are in use, each Astrobee should return to the same berth it
     * originally departed from.<p/>However, using this command is not
     * recommended at this time, because (1) it has not been sufficiently
     * tested (including testing of the QP trajectory planner), (2) at present,
     * the proper berth selection logic is not implemented; instead, Astrobee
     * will always return to berth 1 (= left).
     *
     * Only issue this command if you are sure berth 1 is unoccupied.
     *
     * @param berthNumber Which berth Astrobee is using. 1=left, 2=right.
     * @return PendingResult of this command
     */
    PendingResult autoReturn(int berthNumber);

    /**
     * Docks Astrobee at the specified berth. Preconditions: Astrobee must be
     * near the dock approach point for the specified berth, the berth approach
     * path must not be blocked, and the dock AR tags must not be
     * occluded.<p/>The nominal docking sequence is as follows:<ol><li>Switch
     * to the "MappedLandmarks" localization pipeline (the default for
     * general-purpose navigation).</li><li>Coarsely correct positioning at
     * dock approach point, ensuring dock AR tags are in view.</li><li>Switch
     * to "ARTags" localization pipeline (uses AR tags on dock for more
     * accurate dock-relative localization).</li><li>Fine-tune positioning at
     * dock approach point.</li><li>Complete the docking approach, which should
     * end with Astrobee magnetically retained on the berth, with power and
     * data pins in contact.</li><li>Check magnetic retention by attempting to
     * fly away from the dock. Lack of motion indicates success.</li><li>Idle
     * propulsion.</li><li>Disable localization system.</li></ol>
     *
     * @param berthNumber Which berth the Astrobee is using. 1=left, 2=right.
     * @return PendingResult of this command
     */
    PendingResult dock(int berthNumber);

    /**
     * Idles the propulsion impeller motors.<p/>Astrobee will lose control
     * authority. If it is not grounded in some way (docked or perched), it
     * will drift uncontrolled. Note that the impellers have significant
     * inertia, and may take several seconds to spin down.<p/>This command is
     * usually invoked automatically (e.g., at the end of successful docking or
     * perching). It can also be used as a fault response (e.g., responding to
     * an overspeed fault).
     *
     * @return PendingResult of this command
     */
    PendingResult idlePropulsion();

    /**
     * Perches Astrobee on a handrail. The robot must already be near the
     * appropriate perch approach point for the desired perch location, with
     * the handrail in view of the PerchCam.<p/>The nominal perching sequence
     * is as follows:<ol><li>Switch to the "Handrail" (handrail-relative)
     * localization pipeline.</li><li>Fine-tune positioning at perch approach
     * point.</li><li>Deploy arm.</li><li>Open gripper.</li><li>Move until
     * handrail is in contact, within gripper capture box.</li><li>Close
     * gripper.</li><li>Confirm grasp by attempting to fly away from the
     * handrail. Lack of motion indicates success.</li><li>Idle
     * propulsion.</li></ol>
     *
     * @return PendingResult of this command
     */
    PendingResult perch();

    /**
     * Manually spins up the propulsion modules, so that a future motion
     * command can execute without a spin-up delay. This command is rarely
     * used. Normally, you would just command the desired motion and the
     * spin-up would be triggered automatically if needed. This command is
     * intended to help with synchronizing motion of multiple Astrobees, but
     * that scenario hasn't really been tested.
     *
     * @return PendingResult of this command
     */
    PendingResult prepare();

    /**
     * Moves the robot to the specified pose.<p/>Using the default trapezoidal
     * planner, the move works as follows: <br/>- If holonomic mode is disabled
     * (usual case), the robot will sequentially (1) rotate to face the target
     * position, (2) translate to the target position while facing forward, (3)
     * rotate to the target attitude. <br/>- If holonomic mode is enabled, the
     * robot will simultaneously translate along a straight line to the target
     * position while rotating to the target attitude.
     *
     * @param referenceFrame The reference frame for the target pose.<br/>-
     *                       ISS: The target is expressed in the fixed
     *                       coordinate frame for the current environment
     *                       (which might be the actual ISS, or a lab).<br/>-
     *                       body: The target is expressed relative to the
     *                       robot; for example, setting xyz to (0.5, 0, 0)
     *                       commands 0.5 meters forward motion.
     * @param xyz Target position
     * @param xyzTolerance Not used! Tolerance is dictated by the flight mode.
     *                     This legacy parameter was inherited from the RAPID
     *                     specification.
     * @param rot Target attitude
     * @return PendingResult of this command
     */
    PendingResult simpleMove6DOF(String referenceFrame,
                                 Point xyz,
                                 Vec3d xyzTolerance,
                                 Quaternion rot);

    /**
     * Stops motion of the mobility system and the arm.<p/>Stopping the
     * mobility system means transitioning to active station keeping (the
     * propulsion system is activated if needed). The arm is stopped as in
     * Arm.stopArm (the joints are commanded to maintain their current
     * position, and the gripper is not affected).<p/>Note that there is
     * special behavior when this command is issued by the system monitor as a
     * fault response: in that case, idled propulsion will not be activated.
     * The special behavior ensures that when responding to multiple faults
     * with different responses configured (stopAllMotion vs. idlePropulsion),
     * the idlePropulsion response takes priority.
     *
     * @return PendingResult of this command
     */
    PendingResult stopAllMotion();

    /**
     * Undocks Astrobee.<p/>The nominal undocking sequence is as
     * follows:<ol><li>Switch to the "MappedLandmarks" localization pipeline
     * (the default for general-purpose navigation).</li><li>Spin up
     * impellers.</li><li>Command the dock berth retention magnets to retract,
     * releasing Astrobee. (They will automatically re-extend after a fixed
     * time delay.)</li><li>Move to the dock approach point. (Note: Astrobee
     * attempts to move immediately, without waiting for the berth magnets to
     * fully retract.)</li></ol>
     *
     * @return PendingResult of this command
     */
    PendingResult undock();

    /**
     * Unperches Astrobee from a handrail.<p/>The nominal unperching sequence
     * is as follows:<ol><li>Switch to "MappedLandmarks" localization pipeline
     * (the default for general-purpose navigation).</li><li>Spin up
     * impellers.</li><li>Open gripper, releasing the handrail.</li><li>Move to
     * perch approach point.</li><li>Stow arm, as in Arm.stowArm.</li></ol>
     *
     * @return PendingResult of this command
     */
    PendingResult unperch();

    /**
     * Pauses the running plan. The currently executing command will be aborted
     * (equivalent to Mobility.stopAllMotion), and execution of subsequent
     * commands will be delayed until the plan is resumed with a Plan.runPlan
     * command. See plan.setPlan.
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult pausePlan();

    /**
     * Runs the currently loaded plan. If the plan was previously paused with
     * Plan.pausePlan, this command will resume execution where it previously
     * left off. See plan.setPlan.
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult runPlan();

    /**
     * Loads the most recently uplinked plan.<p/>An Astrobee plan is specified
     * as a JSON-formatted fplan file that contains a list of stations
     * (locations to visit), segments (trajectories for flying between
     * stations), and commands to execute at stations. Plans are generated
     * using the control station plan editor. Sample fplan files can be found
     * in the astrobee/astrobee/plans folder.<p/>The Astrobee control station
     * implements the protocol for managing plans:<ol><li>Uplink a new fplan
     * file using the RAPID compressed file protocol over DDS. (File uplink is
     * not considered a command, so it does not appear in this command
     * dictionary).</li><li>Send this Plan.setPlan command to load the uplinked
     * fplan file. Any errors in the fplan will be reported at this time. Once
     * the plan is loaded, it is initially in the 'paused' state.</li><li>Use
     * the Plan.runPlan / Plan.pausePlan commands to start / stop plan
     * execution. Note that after a plan is paused using Plan.pausePlan,
     * Plan.runPlan will resume plan execution at the beginning of the first
     * plan step that was not fully completed. In the case that a trajectory
     * segment is paused partway through, resuming the plan will cause the
     * robot to fly back to the beginning of the segment and execute the whole
     * segment again. If this behavior is not desired, you can use
     * Plan.skipNextStep.</li></ol>Other related commands
     * include:<ul><li>Plan.skipNextStep: Skips the next step of the currently
     * loaded (and paused) plan.</li><li>Plan.wait: Same as Plan.pausePlan, but
     * execution will resume automatically after the specified wait
     * duration.</li></ul>
     *
     * @return PendingResult of this command
     */
    PendingResult setPlan();

    /**
     * Skips the next step of the currently loaded plan (the step could be a
     * trajectory segment or a command at a station). The plan must be paused.
     * See plan.setPlan.
     *
     * Do not use this command! This is mainly for ground operators.
     *
     * @return PendingResult of this command
     */
    PendingResult skipPlanStep();

    /**
     * Temporarily pauses the running plan. This command only affects plan
     * execution, not individual teleoperated commands. Plan execution will
     * resume automatically after the specified duration. See plan.setPlan.
     *
     * @param duration Seconds to pause
     * @return PendingResult of this command
     */
    PendingResult wait(float duration);

    /**
     * Powers off a component within Astrobee. Note: to turn flashlights on or
     * off, see Settings.setFlashlightBrightness.
     *
     * @param which Which component. Note: 'Front' means 'Forward'.
     * @return PendingResult of this command
     */
    PendingResult powerOffItem(PoweredComponent which);

    /**
     * Powers on a component within Astrobee. Note: to turn flashlights on or
     * off, see Settings.setFlashlightBrightness.
     *
     * @param which Which component. Note: 'Front' means 'Forward'.
     * @return PendingResult of this command
     */
    PendingResult powerOnItem(PoweredComponent which);

    /**
     * Starts the Astrobee intercommunication software. This enables the robot
     * to send and receive a subset of data to and from other Astrobees in the
     * network. Please note, this command must be executed on each robot needed
     * in the communication.
     *
     * @return PendingResult of this command
     */
    PendingResult enableAstrobeeIntercomms();

    /**
     * Sets camera parameters.<p/>The Astrobee camera control life cycle is as
     * follows:<ul><li>When the Astrobee flight software stack is started,
     * recording and streaming are initially disabled for all cameras, and the
     * default camera parameters are as specified in the
     * astrobee/config/cameras.config file.</li><li>For each camera, while
     * recording and streaming are disabled, you may use this
     * Settings.setCamera command to adjust its parameters.</li><li>For each
     * camera, you may enable/disable recording imagery to onboard storage
     * using Settings.setCameraRecording.</li><li>For each camera, you may
     * enable/disable live imagery downlink using
     * Settings.setCameraStreaming.</li></ul>
     *
     * @param cameraName Which camera
     * @param cameraMode Cameras can both stream and record, potentially with
     *                   different parameters (typically, you might downlink at
     *                   lower frame rate and lower resolution). Use this field
     *                   to specify which mode you want to change parameters
     *                   for.
     * @param resolution The resolution of the images to produce, in pixels W x
     *                   H. Specifying resolution smaller than the actual
     *                   sensor resolution causes binning or downsampling. Only
     *                   selected downsampling ratios are supported. Available
     *                   resolutions are:<br/>- SciCam: 1920x1080, 1280x720,
     *                   960x540, 480x270. <br/>- NavCam, DockCam: 1280x960,
     *                   1024x768, 640x480, 320x240. <br/>- HazCam, PerchCam:
     *                   224x171 only
     * @param frameRate Frame rate to send. Maximum frame rate varies by
     *                  camera:<br/>- SciCam: 30 Hz <br/>- NavCam, DockCam: 15
     *                  Hz<br/>- HazCam, PerchCam: 5 Hz
     * @param bandwidth Only used for SciCam. Sets target bitrate used in
     *                  streaming video encoder. Lower bitrate may reduce video
     *                  quality.
     * @return PendingResult of this command
     */
    PendingResult setCamera(CameraName cameraName,
                            CameraMode cameraMode,
                            CameraResolution resolution,
                            float frameRate,
                            float bandwidth);

    /**
     * Sets camera video onboard recording on/off.<p/>Note: For the SciCam,
     * this command actually enables independent recording of H.264 compressed
     * video. For other cameras, this command enables publishing image frames
     * to a topic designated for onboard recording together with other
     * telemetry, but you also need to double-check that the
     * Settings.setDataToDisk configuration will actually log that telemetry
     * topic.<p/>See Settings.setCamera for context.
     *
     * @param cameraName Which camera
     * @param record Set to true/false to enable/disable camera video onboard
     *               recording.
     * @return PendingResult of this command
     */
    PendingResult setCameraRecording(CameraName cameraName, boolean record);

    /**
     * Sets camera streaming live video to the ground on/off.<p/>The SciCam
     * streams H.264 compressed video via RTSP. All other cameras publish
     * independent image frames via DDS.<p/>See Settings.setCamera for
     * context.
     *
     * @param cameraName Which camera
     * @param stream Set to true/false to enable/disable streaming live video
     *               to the ground.
     * @return PendingResult of this command
     */
    PendingResult setCameraStreaming(CameraName cameraName, boolean stream);

    /**
     * Enables/disables obstacle checking
     *
     * @param checkObstacles Set to true/false to enable/disable obstacle
     *                       checking.
     * @return PendingResult of this command
     */
    PendingResult setCheckObstacles(boolean checkObstacles);

    /**
     * Enables/disables keepout zone checking. See Settings.setZones.
     *
     * @param checkZones Set to true/false to enable/disable keepout zone
     *                   checking.
     * @return PendingResult of this command
     */
    PendingResult setCheckZones(boolean checkZones);

    /**
     * Not implemented. This commmand is intended to enable/disable auto-return
     * to the dock, under the assumption that auto-return will eventually be
     * configured in Astrobee's fault table as the automatic fault response for
     * a low-battery fault. The flag is currently ignored because auto-return
     * has not been sufficiently tested and is not used in the fault table. See
     * Mobility.autoReturn.
     *
     * @param enableAutoReturn Set to true/false to enable/disable auto-return.
     * @return PendingResult of this command
     */
    PendingResult setEnableAutoReturn(boolean enableAutoReturn);

    /**
     * For expert use only. Changes the semantics of how Astrobee executes
     * fplan trajectory segments.<p/>A segment specifies the motion trajectory
     * between stations in the plan. Within the segment, desired pose and
     * velocity are smoothly interpolated functions of time over a time
     * interval [t0, t1]. Plans created by the Astrobee control station plan
     * editor always specify each segment's time values relative to the start
     * of that segment's execution (i.e. t0 = 0). These plans must be executed
     * in immediate mode, so called because when execution reaches a new
     * segment, the executive and choreographer immediately 'start the clock'
     * on executing the timed trajectory. In principle, if you want to
     * synchronize motion of multiple robots, it could be useful to have the
     * interval [t0, t1] specified using absolute timestamps. That is the
     * behavior when immediate mode is disabled. The timestamp t0 is
     * interpreted as absolute time using ROS conventions (usually UNIX epoch
     * when running on real hardware, but could be any arbitrary time scale
     * when running in simulation), and the start of motion on the segment
     * would be delayed until the current time t = t0.<p/>At this time, we
     * cannot recommend disabling immediate mode to achieve synchronization,
     * due to the following concerns: (1) execution with absolute timestamps
     * has never really been tested, and may be buggy, (2) the control station
     * plan editor doesn't provide any way to generate segments with absolute
     * timestamps, (3) since it is seldom possible to predict exactly when ISS
     * conditions will be right to begin a multi-robot activity, it would be
     * awkward in practice to have the exact absolute timing of segments
     * hard-coded into the plans.<p/>As an alternative way to synchronize
     * motion, you can execute with immediate mode enabled as usual, but take
     * special care to minimize any skew in start time. Upload the plans in
     * advance, use the Mobility.prepare command to get the robots ready to
     * move, and then run the plans simultaneously. Astrobee to Astrobee
     * communication is in the works, and may eventually enable synchronizing
     * Astrobees in a better way.
     *
     * @param enableImmediate Set to true/false to enable/disable immediate
     *                        mode motion control.
     * @return PendingResult of this command
     */
    PendingResult setEnableImmediate(boolean enableImmediate);

    /**
     * Allows Astrobee to re-plan if it detects an obstacle too close to its
     * forward trajectory. Enabling replanning only makes sense when you are
     * using a planner that is able to plan around obstacles. (See
     * Setting.setPlanner. As of 5/2021, only the QP planner can plan around
     * obstacles).
     *
     * @param enableReplan If true, when Astrobee detects an obstacle too close
     *                     to its forward trajectory, after the robot comes to
     *                     a stop, the choreographer will automatically request
     *                     a new trajectory from the current configured
     *                     planner. If false, the robot will stop and wait for
     *                     operator assistance.
     * @return PendingResult of this command
     */
    PendingResult setEnableReplan(boolean enableReplan);

    /**
     * Set the exposure value for either the nav or dock camera.
     *
     * @param cameraName Which camera
     * @param exposure The value to set the exposurec to.
     * @return PendingResult of this command
     */
    PendingResult setExposure(CameraName cameraName, float exposure);

    /**
     * Sets flashlight brightness.
     *
     * @param which Which flashlight. Note: 'Back' means 'Aft' and 'Front'
     *              means 'Forward'.
     * @param brightness Brightness value between 0 (off) and 1 (full
     *                   brightness). Note that full brightness of an Astrobee
     *                   flashlight is similar to that of an ordinary pocket
     *                   flashlight, and may be uncomfortably bright if pointed
     *                   toward crew eyes. When working with crew, it is
     *                   advisable to use lower brightness values and/or take
     *                   steps to avoid pointing the flashlight toward crew.
     * @return PendingResult of this command
     */
    PendingResult setFlashlightBrightness(FlashlightLocation which,
                                          float brightness);

    /**
     * Enables/disables holonomic mode.<p/>Holonomic mode is sometimes called
     * 'blind flying' because it relaxes the constraint to always point the
     * HazCam in the direction of motion while translating in order to enable
     * obstacle detection. When holonomic mode is enabled, the default
     * trapezoidal planner will simultaneously translate to the target position
     * and rotate to the target attitude.
     *
     * @param enableHolonomic Set to true/false to enable/disable holonomic
     *                        mode.
     * @return PendingResult of this command
     */
    PendingResult setHolonomicMode(boolean enableHolonomic);

    /**
     * Sets mass and inertia parameters for Astrobee control.<p/>The default
     * inertial parameters for each robot are stored in an onboard
     * configuration file, and should not need to be changed as long as the
     * Astrobee remains in its baseline configuration (four batteries and no
     * payloads installed). However, the inertial parameters may vary when the
     * robot configuration changes. Examples are when a payload is installed or
     * reconfigured (like deploying the arm). When configuration changes for a
     * planned activity are known in advance, the standard management approach
     * is to specify them in the inertiaConfig field of the relevant fplan.
     * This command provides the same parameter update capability, but with
     * more flexibility as to when it is applied (for example, you can change
     * in the middle of a plan, during teleoperation, or when commanded by a
     * guest science app).
     *
     * @param name An optional profile name for this inertia profile. The name
     *             has no functional effect, but may be reported in status
     *             telemetry and operator displays.
     * @param mass Mass of the Astrobee assembly
     * @param centerOfMass Center of mass of the Astrobee assembly. Specified
     *                     relative to Astrobee's body frame.
     * @param matrix The moment of inertia tensor. Must be a symmetric matrix.
     *               Specified relative to the center of mass.
     * @return PendingResult of this command
     */
    PendingResult setInertia(String name,
                             float mass,
                             Vec3d centerOfMass,
                             Mat33f matrix);

    /**
     * Sets the map used for localization.
     *
     * @param mapName Full path to the map file to use. 'default' uses the
     *                default map loaded on startup.
     * @return PendingResult of this command
     */
    PendingResult setMap(String mapName);

    /**
     * Changes the value of Astrobee operating limits
     *
     * @param profileName An optional profile name for this set of operating
     *                    limits. The name has no functional effect, but may be
     *                    reported in status telemetry and operator displays.
     * @param flightMode Setting the flight mode updates the GN&C gains, hard
     *                   limits, tolerances, etc., as specified in the world
     *                   config file. See e.g.
     *                   astrobee/astrobee/config/worlds/iss.config. Note that
     *                   executing certain operations, such as docking,
     *                   automatically switches the flight mode as needed.
     *                   Values:<ul><li>Off: Turns off
     *                   propulsion.</li><li>Quiet: Sets propulsion impeller
     *                   speed to QUIET (low speed) with gains and limits tuned
     *                   to safely fly within the reduced
     *                   performance.</li><li>Nominal: Sets propulsion impeller
     *                   speed to NOMINAL (medium speed), with gains and limits
     *                   tuned for typical utility flying.</li><li>Difficult:
     *                   Sets propulsion impeller speed to AGGRESSIVE (high
     *                   speed), with gains and limits tuned for maximum
     *                   performance.</li><li>Precision: Sets propulsion
     *                   impeller speed to AGGRESSIVE, with gains and limits
     *                   tuned for precise motion (slow speed, tight position
     *                   and attitude tolerances).</li></ul>
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
     * Switches which trajectory planner is used when Astrobee needs to
     * generate a trajectory to reach a target pose. Astrobee uses its
     * trajectory planner when it receives a teleoperation motion command.
     * However, when an fplan is generated by the Astrobee control station plan
     * editor, it normally contains pre-computed trajectories for its motion
     * segments; when Astrobee executes these fplans, the onboard trajectory
     * planner is not used.
     *
     * @param planner Which planner to use:<ul><li>Trapezoidal planner:
     *                Generates a straight-line trajectory from the start
     *                position to the target position, using trapezoidal
     *                velocity profiles for translation and rotation. If
     *                holonomic mode is disabled (usual case), the robot will
     *                (1) rotate to face the target position, (2) translate to
     *                the target position while facing forward, (3) rotate to
     *                the target attitude. If holonomic mode is enabled, the
     *                robot will simultaneously translate along a straight line
     *                to the target position while rotating to the target
     *                attitude.</li><li>QP planner: An experimental planner
     *                that generates a curved trajectory from start to target,
     *                avoiding intervening obstacles. As of 10/2020, using the
     *                QP planner is not recommended, because it has not yet
     *                been tested on the ISS.</li></ul>
     * @return PendingResult of this command
     */
    PendingResult setPlanner(PlannerType planner);

    /**
     * Changes the frequency at which one DDS telemetry topic is published.
     *
     * @param telemetryName The DDS telemetry topic to manage. Note: As of Oct
     *                      2020, the "CommStatus" topic is not implemented.
     * @param rate The frequency for publishing the topic.
     * @return PendingResult of this command
     */
    PendingResult setTelemetryRate(TelemetryType telemetryName, float rate);

    /**
     * Loads the most recently uplinked keepout zones file.<p/>The Astrobee
     * control station implements the protocol for managing keepout
     * zones:<ol><li>Uplink a new keepout zone file using the RAPID compressed
     * file protocol over DDS. (File uplink is not considered a command, so it
     * does not appear in this command dictionary).</li><li>Send this
     * Settings.setZones command to load the uplinked zones file. Any errors in
     * the file will be reported at this time.</li><li>Use the
     * Settings.setCheckZones command to enable/disable keepout zone
     * checking.</li></ol>
     *
     * @return PendingResult of this command
     */
    PendingResult setZones();

}
