package gov.nasa.arc.irg.astrobee.api_test_example;

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
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * Created by andres on 9/21/17.
 */

public class ApiTestModelClass {

    private static final String LOGTAG = "API_TEST_CLASS";

    private static final URI ROS_MASTER_URI = URI.create("http://10.0.3.1:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "10.0.3.15";
    private static final String NODE_NAME = "api_test";

    private static final double GOAL_X = 0.5;
    private static final double GOAL_Y = 0.5;
    private static final double GOAL_Z = 0.5;


    // -- To create an instance of this singleton class
    private static ApiTestModelClass singletonInstance;
    RobotFactory factory;
    Robot selfRobot;

    // -- A private constructor to prevent any other class from instantiating.
    private ApiTestModelClass() { }

    public void mainMethod() {
        RobotConfiguration robotConfiguration = new RobotConfiguration();
        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);

        //factory = new DefaultRobotFactory();
        factory = new DefaultRobotFactory(robotConfiguration);
        Log.i(LOGTAG, "My name is: " + factory.getLocalName());

        try {
            Log.i(LOGTAG, "Before get robot!");
            selfRobot = factory.getRobot();
            Log.i(LOGTAG, "Waiting for robot to acquire position...");

            Kinematics kinematics;

            while (true) {
                kinematics = selfRobot.getCurrentKinematics();
                if (kinematics.getConfidence() == Kinematics.Confidence.GOOD) {
                    break;
                }
                Thread.sleep(250);
            }

            Log.i(LOGTAG, "Moving Robot");

            Point currentPosition = kinematics.getPosition();
            Quaternion currentOrientation = kinematics.getOrientation();
            Point endPoint = new Point(currentPosition.getX() + GOAL_X, currentPosition.getY() + GOAL_Y, currentPosition.getZ() + GOAL_Z);

            PendingResult pendingResult = selfRobot.simpleMove6DOF(endPoint, currentOrientation);

            while (!pendingResult.isFinished()) {
                kinematics = selfRobot.getCurrentKinematics();
                Log.i(LOGTAG, "Current Position: " + kinematics.getPosition().toString());
                pendingResult.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pendingResult.getResult();
            Log.i(LOGTAG, "Command Status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                Log.i(LOGTAG, "Command message: " + result.getMessage());
            }

        } catch (AstrobeeException e) {
            Log.e(LOGTAG, "Astrobee Excpt Ooopsies: " + e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted Excpt Ooopsies: " + e);
        } catch (TimeoutException e) {
            Log.e(LOGTAG, "Timeout Excpt Ooopsies: " + e);
        }

        Log.i(LOGTAG, "We're done following order, sir!");

        factory.shutdown();
    }


    // -- Lazy initialization
    public static ApiTestModelClass getSingletonInstance () {
        if (singletonInstance == null) {
            singletonInstance = new ApiTestModelClass();
        }
        return singletonInstance;
    }
}
