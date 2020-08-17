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

package gov.nasa.arc.astrobee.ros.microphone_example;

import java.util.Vector;
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
import gov.nasa.arc.astrobee.ros.guestscience.*;
import gov.nasa.arc.astrobee.ros.internal.util.MessageType;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

// This class implements all operations that the robot with the
// microphone attached to it can do. It is controlled from
// MicrophoneApplication and uses a robot node and a microphone node
// to do its work.
public class MicrophoneImplementation {

    private static final Log logger = LogFactory.getLog(MicrophoneImplementation.class);

    // The IP for the ROS master
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    // IP for the local computer executing this code
    private static final String JAVA_ROS_HOSTNAME = "hlp";
    // Name of the node
    private static final String NODE_NAME = "robot_with_microphone";

    // The instance to access this class
    private static MicrophoneImplementation instance = null;

    // Configuration that will keep data to connect with ROS master.
    private RobotConfiguration m_robotConfiguration = new RobotConfiguration();

    // Instance that will create a robot and a microphone with the given configuration.
    private RobotFactory m_robot_factory = null;
    private MicrophoneFactory m_microphone_factory = null;

    // The robot
    private Robot m_robot = null;

    // The planner to use
    private PlannerType plannerType = null;

    // Store each position and the measured sound intensity
    private java.util.ArrayList< java.util.Vector<Double> > m_recordedSound = null;

    private java.util.ArrayList<Point> m_lawnmowerTrajectory = null;
    private java.util.ArrayList<Point> m_soundSources = null;

    // These determine the allowed region of movement
    private static Point m_minPoint, m_maxPoint;

    private static double samplingRate  = 0.05;
    private boolean m_initialSearchDone = false;
    private boolean m_fastMode = false;
    private boolean m_useGateway = false;

    // Private constructor that prevents other objects from creating
    // instances of this class.  Instances of this class must be
    // provided by a static function (singleton).
    private  MicrophoneImplementation(boolean fastMode, boolean skipGSManager, boolean useGateway) {

        m_fastMode = fastMode;
        m_useGateway = useGateway;

        m_microphone_factory = new MicrophoneFactory();
        m_robot_factory = new DefaultRobotFactory();

        if (!m_useGateway) {

            // Use the JPM module

            // JPM constraints
            m_minPoint = new Point(10.20, -9.75, 4.20);
            m_maxPoint = new Point(11.40, -3.50, 5.60);

            // Points in the lawnmower trajectory
            Point POINT_1 = new Point(10.40, -9.00, 5.40);
            Point POINT_2 = new Point(11.40, -9.00, 5.40);
            Point POINT_3 = new Point(11.40, -8.50, 5.40);
            Point POINT_4 = new Point(10.40, -8.50, 5.40);
            Point POINT_5 = new Point(10.40, -8.00, 5.40);
            Point POINT_6 = new Point(11.40, -8.00, 5.40);
            Point POINT_7 = new Point(11.40, -7.50, 5.40);
            Point POINT_8 = new Point(10.40, -7.50, 5.40);
            // Sound source, just one for now.
            Point source1 = new Point(10.90, -8.25, 6.375);

            Point[] initialTrajectory
                = {POINT_1, POINT_2, POINT_3, POINT_4, POINT_5, POINT_6, POINT_7, POINT_8};

            Point[] soundSources = {source1};

            // Convert to ArrayList
            m_soundSources = new java.util.ArrayList<Point>(java.util.Arrays.asList(soundSources));
            m_lawnmowerTrajectory
                = new java.util.ArrayList<Point>(java.util.Arrays.asList(initialTrajectory));
        }
        else {

            // Use the Gateway module

            // Gateway constraints
            m_minPoint = new Point(8.5,  -8.2, 6.65);
            m_maxPoint = new Point(11.2, -6.6, 7.90);

            Point[] initialTrajectory = {
                // back wall
                new Point(8.5, -8.2, 7.9),
                new Point(8.5, -6.6, 7.9),
                new Point(8.5, -6.6, 7.7),
                new Point(8.5, -8.2, 7.7),
                new Point(8.5, -8.2, 7.5),
                new Point(8.5, -6.6, 7.5),
                new Point(8.5, -6.6, 7.3),
                new Point(8.5, -8.2, 7.3),
                new Point(8.5, -8.2, 7.1),
                new Point(8.5, -6.6, 7.1),
                new Point(8.5, -6.6, 6.95),
                new Point(8.5, -8.2, 6.95),
                new Point(8.5, -8.2, 6.8),
                new Point(8.5, -6.6, 6.8),
                new Point(8.5, -6.6, 6.65),
                new Point(8.5, -8.2, 6.65),
                // side
                new Point(10.9, -8.2, 6.65),
                new Point(10.9, -8.2, 6.8),
                new Point(8.5, -8.2,  6.8),
                new Point(8.5, -8.2,  6.95),
                new Point(11.2, -8.2, 6.95),
                new Point(11.2, -8.2, 7.1),
                new Point(8.5, -8.2, 7.1),
                new Point(8.5, -8.2, 7.3),
                new Point(9.8, -8.2, 7.3),
                new Point(9.8, -8.2, 7.5),
                new Point(8.6, -8.2, 7.5),
                new Point(8.6, -8.2, 7.7),
                new Point(11.2, -8.2, 7.7),
                // bottom
                new Point(11.2, -8.2, 7.9),
                new Point(9.4, -8.2, 7.9),
                new Point(9.4, -8.0, 7.9),
                new Point(10.8, -8.0, 7.9),
                new Point(10.8, -7.8, 7.9),
                new Point(9.4, -7.8, 7.9),
                new Point(9.4, -7.6, 7.9),
                new Point(10.8, -7.6, 7.9),
                new Point(10.8, -7.4, 7.9),
                new Point(9.4, -7.4, 7.9),
                new Point(9.4, -7.2, 7.9),
                new Point(10.8, -7.2, 7.9),
                new Point(10.8, -7.0, 7.9),
                new Point(9.4, -7.0, 7.9),
                new Point(9.4, -6.8, 7.9),
                new Point(10.8, -6.8, 7.9),
                new Point(10.8, -6.6, 7.9),
                new Point(9.4, -6.6, 7.9)
            };

            m_lawnmowerTrajectory
                = new java.util.ArrayList<Point>(java.util.Arrays.asList(initialTrajectory));

            // Gateway sound sources
            double delta1 = 1.18; // larger delta makes colors less red
            double delta2 = 1.18; // larger delta makes colors less red
            double delta3 = 1.21; // larger delta makes colors less red
            Point source1 = new Point(8.5-delta1, -6.9,       7.05);       // back
            Point source2 = new Point(10.0,      -8.2-delta2, 6.75);       // right
            Point source3 = new Point(10.0,      -7.25,       7.9+delta3); // bottom
            Point[] soundSources = {source1, source2, source3};

            // Another path in the Gateway
            // Point POINT_a =  new Point(9.4, -8.2, 6.9);
            // Point POINT_b =  new Point(11.2, -8.2, 6.9);
            // Point POINT_c =  new Point(11.2, -8.2, 7.6);
            // Point POINT_d =  new Point(9.4,  -8.2, 7.6);
            // Point POINT_e =  new Point(9.4,  -8.2, 8.2);
            // Point POINT_f =  new Point(10.6,  -8.2, 8.2);
            // Point POINT_g =  new Point(10.6,  -7.7, 8.2);
            // Point POINT_h =  new Point(9.4,  -7.7, 8.2);
            // Point POINT_i =  new Point(9.4,  -7.2, 8.2);
            // Point POINT_j =  new Point(10.6,  -7.2, 8.2);
            // Point POINT_k =  new Point(10.6,  -6.7, 8.2);
            // Point POINT_l =  new Point(9.4,  -6.7, 8.2);
            // // Another path in the Gateway
            // Point POINT_1 =  new Point(9.2,  -6.6, 8.2);
            // Point POINT_2 =  new Point(9.2,  -8.2, 8.2);
            // Point POINT_3 =  new Point(9.7,  -8.2, 8.2);
            // Point POINT_4 =  new Point(9.7,  -6.6, 8.2);
            // Point POINT_5 =  new Point(10.2, -6.6, 8.2);
            // Point POINT_6 =  new Point(10.2, -8.2, 8.2);
            // Point POINT_7 =  new Point(10.6, -8.2, 8.2);
            // Point POINT_8 =  new Point(10.6, -6.6, 8.2);

            // Convert to ArrayList
            m_soundSources = new java.util.ArrayList<Point>(java.util.Arrays.asList(soundSources));
            m_lawnmowerTrajectory
                = new java.util.ArrayList<Point>(java.util.Arrays.asList(initialTrajectory));
        }

        // Get the robot
        try {
            m_robot = m_robot_factory.getRobot();
        }
        catch (AstrobeeException e) {
            logger.info("Error with Astrobee");
        }
        catch (InterruptedException e) {
            logger.info("Connection Interrupted");
        }

        // Record the sound here
        m_recordedSound = new java.util.ArrayList< java.util.Vector<Double> >();

        // Do all work skipping the guest manager. If m_fastMode is true,
        // also fake the robot.
        if (m_fastMode || skipGSManager) {
            initialSearch();
        }
    }

    // Static method that provides a unique instance of this class
    // @return A unique instance of this class ready to use
    public static MicrophoneImplementation getInstance(boolean fastMode, boolean skipGSManager,
                                                       boolean useGateway) {
        if (instance == null) {
            instance = new MicrophoneImplementation(fastMode, skipGSManager, useGateway);
        }
        return instance;
    }

    // This method sets a default configuration for the robot
    private void configureRobot() {
        // Populating robot configuration
        m_robotConfiguration.setMasterUri(ROS_MASTER_URI);
        m_robotConfiguration.setHostname(JAVA_ROS_HOSTNAME);
        m_robotConfiguration.setNodeName(NODE_NAME);
    }

    // This method shutdown the robot factory in order to allow java to close correctly.
    public void shutdownFactory() {
        // TODO(oalexan1) The app does not exit gracefully. Need to understand why.
        m_robot_factory.shutdown();
        m_microphone_factory.shutdown();
    }

    public void initialSearch() {

        // Apparently this should happen early.
        Kinematics k = getTrustedRobotKinematics();
        //logger.info("Kinematics position is " + k.getPosition());

        // Set the default planner.
        setPlanner(PlannerType.TRAPEZOIDAL);

        // For gateway no zones were defined yet.
        m_robot.setCheckZones(false);

        // Do a preliminary trajectory in a lawnmower pattern.
        Point startPoint = k.getPosition();
        runTrajectory(startPoint, m_lawnmowerTrajectory);
        m_initialSearchDone = true;
    }

    public boolean refinedSearch() {

        if (!m_initialSearchDone) {
            logger.error("Must do an initial search first.");
            return false;
        }

        // Find the loudest sound so far
        Point loudest = findHighestSoundLocation(m_recordedSound);

        // Spiral around that location to find the sound source
        java.util.ArrayList<Point> spiralTrajectory =
            genSpiralTrajectory(loudest, m_minPoint, m_maxPoint);

        // Start where the last trajectory left off. This is fragile.
        // We don't actually use this start point unless in fast
        // mode. Otherwise we query the robot itself.
        Point startPoint = m_lawnmowerTrajectory.get(m_lawnmowerTrajectory.size() - 1);

        runTrajectory(startPoint, spiralTrajectory);

        // Find the loudest sound so far
        Point loudest2 = findHighestSoundLocation(m_recordedSound);

        return true;
    }

    // Examine all the sound recording and return the position of the loudest.
    static public Point findHighestSoundLocation(java.util.ArrayList< java.util.Vector<Double> >
                                                 recordedSound) {
        double maxVal = -1.0;
        Point loc = new Point(0.0, 0.0, 0.0);
        if (recordedSound == null || recordedSound.size() == 0) {
            logger.error("No measurements exist.");
            return loc;
        }

        for (int it = 0; it < recordedSound.size(); it++) {
            double val = recordedSound.get(it).get(3);
            if (val < maxVal)
                continue;
            maxVal = val;
            loc = new Point(recordedSound.get(it).get(0),
                            recordedSound.get(it).get(1),
                            recordedSound.get(it).get(2));

            // logger.info("Loudest so far: " + loc.toString() + " " + Double.toString(maxVal));
        }
        return loc;
    }

    public static double pointDistance(Point P, Point Q) {
        double dx = Q.getX() - P.getX();
        double dy = Q.getY() - P.getY();
        double dz = Q.getZ() - P.getZ();
        double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
        return len;
    }

    // Generate a spiral trajectory around the current point.
    // This spiral is in the x-y plane.
    // There are hard-coded values here that need to be moved out.
    static public java.util.ArrayList<Point> genSpiralTrajectory(Point spiralCenter,
                                                                 Point minPoint,
                                                                 Point maxPoint) {

        java.util.ArrayList<Point> trajectory = new java.util.ArrayList<Point>();

        double maxDist = 0.5;    // how far to go from spiral center
        double numRot  = 2.9;    // number of full rotations in the spiral
        double epsilon = 0.001;  // start with many points, but then sample them


        int N = (int)(maxDist/epsilon);
        Point prevPt = null;
        for (int it = 0; it < N; it++) {

            double radius = epsilon * it; // it goes from 0 to maxDist
            double angle  = 1.5 * Math.PI * numRot * (radius/maxDist);
            double xval   = spiralCenter.getX() + radius * Math.cos(angle);
            double yval   = spiralCenter.getY() + radius * Math.sin(angle);
            double zval   = spiralCenter.getZ();

            // Make sure we don't run out of bounds
            if (xval < minPoint.getX()) xval = minPoint.getX();
            if (xval > maxPoint.getX()) xval = maxPoint.getX();
            if (yval < minPoint.getY()) yval = minPoint.getY();
            if (yval > maxPoint.getY()) yval = maxPoint.getY();
            if (zval < minPoint.getZ()) zval = minPoint.getZ();
            if (zval > maxPoint.getZ()) zval = maxPoint.getZ();

            Point pt = new Point(xval, yval, zval);
            if (prevPt == null || pointDistance(pt, prevPt) > samplingRate) {
                prevPt = pt;
                trajectory.add(pt);
            }
        }

        return trajectory;
    }

    // Robot orientation along a given segment.
    // This logic is copied from the trapezoidal planner nodelet
    // in C++.
    public Quaternion orientationAlongSegment(Point beg, Point end) {

        // Use Apache vectors and matrices to do the math.

        Vector3D b = new Vector3D(beg.getX(), beg.getY(), beg.getZ());
        Vector3D e = new Vector3D(end.getX(), end.getY(), end.getZ());

        Vector3D vfwd = e.subtract(b);

        double len = vfwd.getNorm();
        if (len > 0)
            vfwd = vfwd.normalize();
        else
            vfwd = new Vector3D(1.0, 0.0, 0.0);

        Vector3D vdown  = new Vector3D(0.0, 0.0, 1.0);
        Vector3D vright = new Vector3D(0.0, 1.0, 0.0);

        // Check that the direction of motion is not along the Z axis. In this
        // case the approach of taking the cross product with the world Z will
        // fail and we need to choose a different axis.
        double epsilon = 1.0e-3;
        if (Math.abs(vdown.dotProduct(vfwd)) < 1.0 - epsilon) {
            vright = vdown.crossProduct(vfwd);
            vdown = vfwd.crossProduct(vright);
            if (vdown.getZ() < 0) {
                vright = vright.negate();
                vdown = vfwd.crossProduct(vright);
            }
        }
        else {
            vdown = vfwd.crossProduct(vright);
            vright = vdown.crossProduct(vfwd);
            if (vright.getY() < 0) {
                vdown = vdown.negate();
                vright = vdown.crossProduct(vfwd);
            }
        }

        // Make sure all vectors are normalized
        vfwd   = vfwd.normalize();
        vright = vright.normalize();
        vdown  = vdown.normalize();

        // Construct a rotation matrix
        double dcm[][] = new double[3][3];
        dcm[0][0] = vfwd.getX(); dcm[0][1] = vright.getX(); dcm[0][2] = vdown.getX();
        dcm[1][0] = vfwd.getY(); dcm[1][1] = vright.getY(); dcm[1][2] = vdown.getY();
        dcm[2][0] = vfwd.getZ(); dcm[2][1] = vright.getZ(); dcm[2][2] = vdown.getZ();
        Rotation R = new Rotation(dcm, 1e-8);

        //Print the rotation matrix
        //double d[][] = R.getMatrix();
        //for (int row = 0; row < 3; row++) {
        //    logger.info("\nMatrix is " +
        //		Float.toString((float)d[row][0]) + " " +
        //		Float.toString((float)d[row][1]) + " " +
        //		Float.toString((float)d[row][2]));
        //}

        return new Quaternion(-(float)R.getQ1(), -(float)R.getQ2(),
                              -(float)R.getQ3(), (float)R.getQ0());
    }

    // Tell the robot to move along a given trajectory.
    private void runTrajectory(Point startPoint, java.util.ArrayList<Point> trajectory) {

        // Loop through the points and orientations previously defined.
        for (int i = 0; i < trajectory.size(); i++) {

            if (!m_fastMode) {
                // Do an honest job
                Kinematics k = m_robot.getCurrentKinematics();
                Point beg;
                if (i == 0)
                    beg = k.getPosition();   // current position
                else
                    beg = trajectory.get(i-1);
                Point end = trajectory.get(i); // next position

                // The bot does  not like to move between close points
                if (pointDistance(beg, end) < samplingRate/2.0)
                    continue;

                Quaternion orientation = orientationAlongSegment(beg, end);

                Result result = moveTo(beg, orientation);
                if (!result.hasSucceeded()) {
                    // If anything fails we cancel all execution.
                    logger.error("Motion failed.");
                    break;
                }
                result = moveTo(end, orientation);
                if (!result.hasSucceeded()) {
                    // If anything fails we cancel all execution.
                    logger.error("Motion failed.");
                    break;
                }
            }
            else {
                // Bypass all the bot logic, and quickly move along the points
                // while measuring the sound there.
                Point beg;
                if (i == 0)
                    beg = startPoint;
                else
                    beg = trajectory.get(i-1);
                Point end = trajectory.get(i);

                // Since we won't use an actual bot that will slowly
                // takes samples along the way, fake it by creating
                // some sample points.
                double spacing = 0.1; // In meters. This needs tuning.
                double dx = end.getX() - beg.getX();
                double dy = end.getY() - beg.getY();
                double dz = end.getZ() - beg.getZ();
                double len = Math.sqrt(dx*dx + dy*dy + dz*dz);
                int num = (int)Math.round(len/spacing) + 1;

                for (int pt = 0; pt <= num; pt++) {
                    double t = (double)pt/(double)num;
                    Point P = new Point(beg.getX() + t*dx,
                                        beg.getY() + t*dy,
                                        beg.getZ() + t*dz);
                    genSound(P);
                    try {
                        // Sleep a little so that ROS can keep up
                        Thread.sleep(100);
                    }
                    catch (InterruptedException e) {
                    }
                }

            }
        }
    }

    // Compute and publish the sound intensity
    private boolean genSound(Point P) {

        if (m_soundSources == null || m_soundSources.size() <= 0) {
            logger.error("Sound sources were not set.");
            return false;
        }

        // Measure the sound
        double soundIntensity = 0.0;
        for (int it = 0; it < m_soundSources.size(); it++) {
            Point S = m_soundSources.get(it);
            double dx = S.getX() - P.getX();
            double dy = S.getY() - P.getY();
            double dz = S.getZ() - P.getZ();
            double d  = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (d == 0.0)
                d = 1e-10;  // Avoid underflow
            soundIntensity += 1.0/d/d; // Inverse square law
        }

        logger.info("Raw sound " + Double.toString(soundIntensity));
        // Do not let the sound be bigger than 1 for plotting purposes.
        if (soundIntensity > 1.0)
            soundIntensity = 1.0;

        // To have the colors scale nicely. This is a hack.
        double minInten1 = -0.2, minInten2 = 0.3;
        if (soundIntensity < minInten1)
            soundIntensity = minInten1;
        soundIntensity = (soundIntensity - minInten1)/(1.0 - minInten1);
        soundIntensity = Math.pow(soundIntensity, 16.0);
        if (soundIntensity < minInten2)
            soundIntensity = minInten2;

        // Store the 3D point and sound intensity in a vector
        java.util.Vector<Double> ps = new java.util.Vector<Double>();
        ps.add(P.getX());
        ps.add(P.getY());
        ps.add(P.getZ());
        ps.add(soundIntensity);

        // Publish the sound
        MicrophoneNode microphoneNode = m_microphone_factory.getMicrophoneNode();
        microphoneNode.publishSound(ps);

        m_recordedSound.add(ps); // store the measured position and sound

        logger.info("Current position and scaled sound: " + P.toString()
                    + " " + Double.toString(soundIntensity));
        return true;
    }

    // Record the position and sound while the robot is moving.
    private Result getCommandResult(PendingResult pending, boolean printRobotPosition) {

        Result result = null;

        try {
            Kinematics k;

            // Waiting until command is done.
            while (!pending.isFinished()) {
                if (printRobotPosition) {
                    // Meanwhile, let's get the positioning along the trajectory
                    k = m_robot.getCurrentKinematics();

                    Point P = k.getPosition();
                    if (!genSound(P))
                        return result;

                    //logger.info("Current orientation: " + k.getOrientation().toString());
                }

                // Wait a little bit before retry
                pending.getResult(1000, TimeUnit.MILLISECONDS);
            }

            // Getting final result
            result = pending.getResult();

            // Print result in the log.
            printLogCommandResult(result);

        }
        catch (AstrobeeException e) {
            logger.error("Error with Astrobee.");
        }
        catch (InterruptedException e) {
            logger.error("Connection interrupted.");
        }
        catch (TimeoutException e) {
            logger.error("Connection timeout.");
        } finally {
            // Return command execution result.
            return result;
        }
    }

    // Get trusted data related to the motion, positioning and orientation for Astrobee
    public Kinematics getTrustedRobotKinematics() {
        logger.info("Waiting for robot to acquire position.");

        // Variable that will keep all data related to positioning and movement.
        Kinematics k;

        // Waiting until we get a trusted kinematics
        while (true) {
            // Get kinematics
            k = m_robot.getCurrentKinematics();

            // Is it good?
            if (k.getConfidence() == Kinematics.Confidence.GOOD)
                // Don't wait anymore, move on.
                break;

            // It's not good, wait a little bit and try again
            try {
                Thread.sleep(250);
            }
            catch (InterruptedException e) {
                logger.error("It was not possible to get a trusted kinematics.");
                return null;
            }
        }

        return k;
    }

    // Move Astrobee to the given point and rotate it to the given orientation.
    //
    // @param goalPoint   Absolute cardinal point (xyz)
    // @param orientation An instance of the Quaternion class.
    //                    You may want to use INITIAL_POSITION as an example.
    // @return A Result instance carrying data related to the execution.
    // Returns null if the command was NOT execute as a result of an error
    public Result moveTo(Point goalPoint, Quaternion orientation) {

        // First, stop all motion
        Result result = stopAllMotion();

        if (result.hasSucceeded()) {
            // We stopped, do your stuff now
            //logger.info("Planner is " + plannerType.toString());
            //logger.info("Moving the bee.");

            // Setting a simple movement command using the end point and the end orientation.
            PendingResult pending = m_robot.simpleMove6DOF(goalPoint, orientation);

            // Get the command execution result and send it back to the requester.
            result = getCommandResult(pending, true);
        }

        return result;
    }

    public Result stopAllMotion() {
        PendingResult pendingResult = m_robot.stopAllMotion();
        return getCommandResult(pendingResult, false);
    }

    // An optional method used to print command execution results on the Android log
    // @param result
    private void printLogCommandResult(Result result) {
        logger.info("Command status: " + result.getStatus().toString());

        // In case command fails
        if (!result.hasSucceeded()) {
            logger.info("Command message: " + result.getMessage());
        }
    }

    private boolean setPlanner(PlannerType plannerType) {
        PendingResult pendingPlanner = m_robot.setPlanner(plannerType);
        Result result = getCommandResult(pendingPlanner, false);
        if (result.hasSucceeded()) {
            this.plannerType = plannerType;
            logger.info("Planner set to: " + plannerType);
        }
        return result.hasSucceeded();
    }

}
