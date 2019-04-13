
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

import android.util.Log;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import gov.nasa.arc.astrobee.AstrobeeException;
import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.Robot;
import gov.nasa.arc.astrobee.RobotFactory;
import gov.nasa.arc.astrobee.ros.DefaultRobotFactory;
import gov.nasa.arc.astrobee.ros.RobotConfiguration;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * A simple API implementation class that provides an easier way to work with the Astrobee API
 */

public class ApiCommandImplementation {

    // Constants that represent the axis in a 3D world
    public static final int X_AXIS = 2;
    public static final int Y_AXIS = 3;
    public static final int Z_AXIS = 4;

    // Center of US Lab
    public static final Point CENTER_US_LAB = new Point(2, 0, 4.8);

    // Default values for the Astrobee's arm
    public static final float ARM_TILT_DEPLOYED_VALUE = 0f;
    public static final float ARM_TILT_STOWED_VALUE = 180f;
    public static final float ARM_PAN_DEPLOYED_VALUE = 0f;
    public static final float ARM_PAN_STOWED_VALUE = 0f;

    // Constants needed to connect with ROS master
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "hlp";

    // Set the name of the app as the node name
    private static final String NODE_NAME = "test_simple_trajectory";

    // The instance to access this class
    private static ApiCommandImplementation instance = null;

    // Configuration that will keep data to connect with ROS master
    private RobotConfiguration robotConfiguration = new RobotConfiguration();

    // Instance that will create a robot with the given configuration
    private RobotFactory factory;

    // The robot itself
    private Robot robot;

    // The planner to be used (QP, TRAPEZOIDAL)
    private PlannerType plannerType = null;

    /**
     * Private constructor that prevents other objects from creating instances of this class.
     * Instances of this class must be provided by a static function (Singleton).
     *
     * DO NOT call any Astrobee API function inside this method since the API might not be ready
     * to issue commands.
     */
    private ApiCommandImplementation() {
        // Set up ROS configuration
        configureRobot();

        // Get the factory in order to access the robot.
        factory = new DefaultRobotFactory(robotConfiguration);

        try {
            // Get the robot
            robot = factory.getRobot();

        } catch (AstrobeeException e) {
            Log.e("LOG", "Error with Astrobee");
        } catch (InterruptedException e) {
            Log.e("LOG", "Connection Interrupted");
        }
    }

    /**
     * Static method that provides a unique instance of this class
     *
     * @return A unique instance of this class ready to use
     */
    public static ApiCommandImplementation getInstance() {
        if (instance == null) {
            instance = new ApiCommandImplementation();
        }
        return instance;
    }

    /**
     * This method sets a default configuration for the robot
     */
    private void configureRobot() {
        // Populating robot configuration
        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);
    }

    /**
     * This method shutdown the robot factory in order to allow java to close correctly.
     */
    public void shutdownFactory() {
        factory.shutdown();
    }

    /**
     * This method waits for a pending task and returns the result.
     *
     * @param pending Pending task being executed
     * @param printRobotPosition Should it print robot kinematics while waiting for task to be
     *                           completed?
     * @param timeout Number of seconds before stopping to wait for result (-1 for no timeout,
     *                0 will loop once).
     * @return Pending task result. NULL if an internal error occurred or request timeout.
     */
    public Result getCommandResult(PendingResult pending, boolean printRobotPosition, int timeout) {

        Result result = null;
        int counter = 0;

        try {
            Kinematics k;

            // Waiting until command is done.
            while (!pending.isFinished()) {
                if (timeout >= 0) {
                    // There is a timeout setting
                    if (counter > timeout)
                        return null;
                }

                if (printRobotPosition) {
                    // Meanwhile, let's get the positioning along the trajectory
                    k = robot.getCurrentKinematics();
                    Log.i("LOG", "Current Position: " + k.getPosition().toString());
                    Log.i("LOG", "Current Orientation" + k.getOrientation().toString());
                }

                // Wait 1 second before retrying
                pending.getResult(1000, TimeUnit.MILLISECONDS);
                counter++;
            }

            // Getting final result
            result = pending.getResult();

            // Print result in the log.
            printLogCommandResult(result);

        } catch (AstrobeeException e) {
            Log.e("LOG", "Error with Astrobee");
        } catch (InterruptedException e) {
            Log.e("LOG", "Connection Interrupted");
        } catch (TimeoutException e) {
            Log.e("LOG", "Timeout connection");
        } finally {
            // Return command execution result.
            return result;
        }
    }

    /**
     * Get trusted data related to positioning and orientation for Astrobee
     *
     * @param timeout Number of seconds before canceling request
     * @return Trusted Kinematics, null if an internal error occurred or request timeout
     */
    public Kinematics getTrustedRobotKinematics(int timeout) {
        Log.e("LOG", "Waiting for robot to acquire position");

        // Variable that will keep all data related to positioning and movement.
        Kinematics k = null;

        int count = 0;

        // Waiting until we get a trusted kinematics
        while (count <= timeout || timeout == -1) {
            // Get kinematics
            k = robot.getCurrentKinematics();

            // Is it good?
            if (k.getConfidence() == Kinematics.Confidence.GOOD)
                // Don't wait anymore, move on.
                break;
            else
                k = null;

            // It's not good, wait a little bit and try again
            try {
                Thread.sleep(1000);
                count++;
            } catch (InterruptedException e) {
                Log.e("LOG", "It was not possible to get a trusted kinematics. Sorry");
                return null;
            }
        }

        return k;
    }

    public Kinematics getTrustedRobotKinematics() {
        return getTrustedRobotKinematics(-1);
    }

    /**
     * It moves Astrobee to the given point and rotate it to the given orientation.
     *
     * @param goalPoint   Absolute cardinal point (xyz)
     * @param orientation An instance of the Quaternion class.
     *                    You may want to use CENTER_US_LAB or CENTER_JEM as an example depending
     *                    on your initial position.
     * @return A Result instance carrying data related to the execution.
     * Returns null if the command was NOT execute as a result of an error
     */
    public Result moveTo(Point goalPoint, Quaternion orientation) {

        // Intent to set planner trapezoidal
        setPlanner(PlannerType.TRAPEZOIDAL);

        // Stop all motion
        Result result = stopAllMotion();

        if (result.hasSucceeded()) {
            // We stopped, do your stuff now
            Log.i("LOG", "Planner is " + plannerType.toString());

            Log.e("LOG", "Moving the bee");

            // Setting a simple movement command using the end point and the end orientation.
            PendingResult pending = robot.simpleMove6DOF(goalPoint, orientation);

            // Get the command execution result and send it back to the requester.
            result = getCommandResult(pending, true, -1);
        }

        return result;
    }

    /**
     * It moves Astrobee to the given point using a relative reference
     * and rotates it to the given orientation.
     *
     * @param goalPoint   The relative end point (relative to Astrobee)
     * @param orientation The absolute orientation
     * @return
     */
    public Result relativeMoveTo(Point goalPoint, Quaternion orientation) {

        // Ger current position
        Kinematics k = getTrustedRobotKinematics(5);
        if (k == null) {
            return null;
        }

        Point currPosition = k.getPosition();

        Point endPoint = new Point(
                currPosition.getX() + goalPoint.getX(),
                currPosition.getY() + goalPoint.getY(),
                currPosition.getZ() + goalPoint.getZ()
        );

        return moveTo(endPoint, orientation);
    }

    public Result relativeMoveInAxis(int axis, double nMeters, Quaternion orientation) {
        Point endPoint = new Point(
                axis == X_AXIS ? nMeters : 0,
                axis == Y_AXIS ? nMeters : 0,
                axis == Z_AXIS ? nMeters : 0
        );

        return relativeMoveTo(endPoint, orientation);
    }

    public Result stopAllMotion() {
        PendingResult pendingResult = robot.stopAllMotion();
        return getCommandResult(pendingResult, false, -1);
    }

    public Result dock() {
        PendingResult pendingResult = robot.dock(1);
        return getCommandResult(pendingResult, true, -1);
    }

    public Result undock() {
        PendingResult pendingResult = robot.undock();
        return getCommandResult(pendingResult, true, -1);
    }

    /**
     * An optional method used to print command execution results on the Android log
     * @param result
     */
    private void printLogCommandResult(Result result) {
        Log.e("LOG", "Command status: " + result.getStatus().toString());

        // In case command fails
        if (!result.hasSucceeded()) {
            Log.e("LOG", "Command message: " + result.getMessage());
        }

        Log.e("LOG", "Done");
    }

    /**
     * Method to get the robot from this API Implementation.
     * @return
     */
    public Robot getRobot() {
        return robot;
    }

    public boolean setPlanner(PlannerType plannerType) {
        PendingResult pendingPlanner = robot.setPlanner(plannerType);
        Result result = getCommandResult(pendingPlanner, false, 5);
        if (result.hasSucceeded()) {
            this.plannerType = plannerType;
            Log.i("LOG", "Planner set to " + plannerType);
        }

        return result.hasSucceeded();
    }
}
