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
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

/**
 * Created by andres mora on 9/21/17.
 */

public class ApiCommandImplementation {

    private static final String LOGTAG = "API_CMD_IMPL_CLASS";

    private static final URI ROS_MASTER_URI = URI.create("http://10.0.3.1:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "10.0.3.15";
    private static final String NODE_NAME = "api_test";

    // -- To create an instance of this singleton class
    private static ApiCommandImplementation singletonInstance;

    private Robot selfRobot;
    private RobotConfiguration robotConfiguration = new RobotConfiguration();
    private RobotFactory factory; // = new DefaultRobotFactory(robotConfiguration);


    // -- A private constructor to prevent any other class from instantiating.
    private ApiCommandImplementation() { }

    public void moveTo(double goalX, double goalY, double goalZ) {
        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);

        factory = new DefaultRobotFactory(robotConfiguration);
        //Log.i(LOGTAG, "My name is: " + factory.getLocalName());
        MainActivity.setmTxt_msgRcvdToRobot("My name is: " + factory.getLocalName());

        try {
            //Log.i(LOGTAG, "Before get robot!");
            MainActivity.setmTxt_msgRcvdToRobot("Before get robot!");
            selfRobot = factory.getRobot();

            //Log.i(LOGTAG, "Waiting for robot to acquire position...");
            MainActivity.setmTxt_msgRcvdToRobot("Waiting for robot to acquire position...");
            Kinematics kinematics;

            while (true) {
                kinematics = selfRobot.getCurrentKinematics();
                if (kinematics.getConfidence() == Kinematics.Confidence.GOOD) {
                    break;
                }
                Thread.sleep(250);
            }

            //Log.i(LOGTAG, "Moving Robot");
            //Log.i(LOGTAG, "goalX: " + goalX + ", goalY: " + goalY + ", goalZ: " + goalZ);
            MainActivity.setmTxt_msgRcvdToRobot("Moving Robot");
            MainActivity.setmTxt_msgRcvdToRobot("goalX: " + goalX + ", goalY: " + goalY + ", goalZ: " + goalZ);


            Point currentPosition = kinematics.getPosition();
            Quaternion currentOrientation = kinematics.getOrientation();
            Point endPoint = new Point(currentPosition.getX() + goalX, currentPosition.getY() + goalY, currentPosition.getZ() + goalZ);

            PendingResult pendingResult = selfRobot.simpleMove6DOF(endPoint, currentOrientation);

            while (!pendingResult.isFinished()) {
                kinematics = selfRobot.getCurrentKinematics();
                //Log.i(LOGTAG, "Current Position: " + kinematics.getPosition().toString());
                MainActivity.setmTxt_msgRcvdToRobot("Current Position: " + kinematics.getPosition().toString());
                pendingResult.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pendingResult.getResult();
            //Log.i(LOGTAG, "Command Status: " + result.getStatus().toString());
            MainActivity.setmTxt_msgRcvdToRobot("Command Status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                Log.i(LOGTAG, "Command message: " + result.getMessage());
            }

        } catch (AstrobeeException e) {
            Log.e(LOGTAG, "Astrobee Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Astrobee Excpt Ooopsies: " + e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Interrupted Excpt Ooopsies: " + e);
        } catch (TimeoutException e) {
            Log.e(LOGTAG, "Timeout Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Timeout Excpt Ooopsies: " + e);
        }

        //Log.i(LOGTAG, "We're done following order, sir!");
        MainActivity.setmTxt_msgRcvdToRobot("We're done following order, sir!");

        factory.shutdown();
    }

    public void armTest(float var1, float var2, ActionType var3) {

        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);

        factory = new DefaultRobotFactory(robotConfiguration);
        //Log.i(LOGTAG, "My name is: " + factory.getLocalName());
        MainActivity.setmTxt_msgRcvdToRobot("My name is: " + factory.getLocalName());

        try {
            //Log.i(LOGTAG, "Before get robot!");
            selfRobot = factory.getRobot();

            //Log.i(LOGTAG, "Waiting for robot to acquire position...");
            MainActivity.setmTxt_msgRcvdToRobot("Waiting for robot to acquire position...");
            Kinematics kinematics;

            while (true) {
                kinematics = selfRobot.getCurrentKinematics();
                if (kinematics.getConfidence() == Kinematics.Confidence.GOOD) {
                    break;
                }
                Thread.sleep(250);
            }

            //Log.i(LOGTAG, "Moving Robot Arm");
            MainActivity.setmTxt_msgRcvdToRobot("Moving Robot Arm");

            float currentAngle_1 = 0.0f;
            float currentAngle_2 = 0.0f;

            PendingResult pendingResult = selfRobot.armPanAndTilt(currentAngle_1 + var1, currentAngle_2 + var2, var3);

            while (!pendingResult.isFinished()) {
                kinematics = selfRobot.getCurrentKinematics();
                //Log.i(LOGTAG, "Current Position: " + kinematics.getPosition().toString());
                MainActivity.setmTxt_msgRcvdToRobot("Current Position: " + kinematics.getPosition().toString());
                pendingResult.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pendingResult.getResult();
            //Log.i(LOGTAG, "Command Status: " + result.getStatus().toString());
            MainActivity.setmTxt_msgRcvdToRobot("Command Status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                Log.i(LOGTAG, "Command message: " + result.getMessage());
                MainActivity.setmTxt_msgRcvdToRobot("Command message: " + result.getMessage());
            }

        } catch (AstrobeeException e) {
            Log.e(LOGTAG, "Astrobee Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Astrobee Excpt Ooopsies: " + e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Interrupted Excpt Ooopsies: " + e);
        } catch (TimeoutException e) {
            Log.e(LOGTAG, "Timeout Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Timeout Excpt Ooopsies: " + e);
        }

        //Log.i(LOGTAG, "We're done following order, sir!");
        MainActivity.setmTxt_msgRcvdToRobot("We're done following order, sir!");
        factory.shutdown();

    }

    public void dockTest(int var1) {

        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);

        factory = new DefaultRobotFactory(robotConfiguration);
        //Log.i(LOGTAG, "My name is: " + factory.getLocalName());
        MainActivity.setmTxt_msgRcvdToRobot("My name is: " + factory.getLocalName());

        try {
            //Log.i(LOGTAG, "Before get robot!");
            selfRobot = factory.getRobot();

            //Log.i(LOGTAG, "Waiting for robot to acquire position...");
            MainActivity.setmTxt_msgRcvdToRobot("Waiting for robot to acquire position...");
            Kinematics kinematics;

            while (true) {
                kinematics = selfRobot.getCurrentKinematics();
                if (kinematics.getConfidence() == Kinematics.Confidence.GOOD) {
                    break;
                }
                Thread.sleep(250);
            }

            //Log.i(LOGTAG, "Docking Robot");
            MainActivity.setmTxt_msgRcvdToRobot("Docking Robot");

            PendingResult pendingResult = selfRobot.dock(var1);

            while (!pendingResult.isFinished()) {
                kinematics = selfRobot.getCurrentKinematics();
                Log.i(LOGTAG, "Current Position: " + kinematics.getPosition().toString());
                MainActivity.setmTxt_msgRcvdToRobot("Current Position: " + kinematics.getPosition().toString());
                pendingResult.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pendingResult.getResult();
            //Log.i(LOGTAG, "Command Status: " + result.getStatus().toString());
            MainActivity.setmTxt_msgRcvdToRobot("Command Status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                //Log.i(LOGTAG, "Command message: " + result.getMessage());
                MainActivity.setmTxt_msgRcvdToRobot("Command message: " + result.getMessage());
            }

        } catch (AstrobeeException e) {
            Log.e(LOGTAG, "Astrobee Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Astrobee Excpt Ooopsies: " + e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Interrupted Excpt Ooopsies: " + e);
        } catch (TimeoutException e) {
            Log.e(LOGTAG, "Timeout Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Timeout Excpt Ooopsies: " + e);
        }

        //Log.i(LOGTAG, "We're done following order, sir!");
        MainActivity.setmTxt_msgRcvdToRobot("We're done following order, sir!");
        factory.shutdown();

    }

    public void lightTest(FlashlightLocation var1, float var2) {

        robotConfiguration.setMasterUri(ROS_MASTER_URI);
        robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        robotConfiguration.setNodeName(NODE_NAME);

        factory = new DefaultRobotFactory(robotConfiguration);
        //Log.i(LOGTAG, "My name is: " + factory.getLocalName());
        MainActivity.setmTxt_msgRcvdToRobot("My name is: " + factory.getLocalName());

        try {
            //Log.i(LOGTAG, "Before get robot!");
            selfRobot = factory.getRobot();

            //Log.i(LOGTAG, "Waiting for robot to acquire position...");
            MainActivity.setmTxt_msgRcvdToRobot("Waiting for robot to acquire position...");
            Kinematics kinematics;

            while (true) {
                kinematics = selfRobot.getCurrentKinematics();
                if (kinematics.getConfidence() == Kinematics.Confidence.GOOD) {
                    break;
                }
                Thread.sleep(250);
            }

            //Log.i(LOGTAG, "Lighting up the Robot");
            MainActivity.setmTxt_msgRcvdToRobot("Lighting up the Robot");
            PendingResult pendingResult = selfRobot.setFlashlightBrightness(var1, var2);

            while (!pendingResult.isFinished()) {
                kinematics = selfRobot.getCurrentKinematics();
                Log.i(LOGTAG, "Current Position: " + kinematics.getPosition().toString());
                MainActivity.setmTxt_msgRcvdToRobot("Current Position: " + kinematics.getPosition().toString());
                pendingResult.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pendingResult.getResult();
            //Log.i(LOGTAG, "Command Status: " + result.getStatus().toString());
            MainActivity.setmTxt_msgRcvdToRobot("Command Status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                //Log.i(LOGTAG, "Command message: " + result.getMessage());
                MainActivity.setmTxt_msgRcvdToRobot("Command message: " + result.getMessage());
            }

        } catch (AstrobeeException e) {
            Log.e(LOGTAG, "Astrobee Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Astrobee Excpt Ooopsies: " + e);
        } catch (InterruptedException e) {
            Log.e(LOGTAG, "Interrupted Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Interrupted Excpt Ooopsies: " + e);
        } catch (TimeoutException e) {
            Log.e(LOGTAG, "Timeout Excpt Ooopsies: " + e);
            MainActivity.setmTxt_msgRcvdToRobot("Timeout Excpt Ooopsies: " + e);
        }

        //Log.i(LOGTAG, "We're done following order, sir!");
        MainActivity.setmTxt_msgRcvdToRobot("We're done following order, sir!");
        factory.shutdown();
    }


    // -- Lazy initialization
    public static ApiCommandImplementation getSingletonInstance () {
        if (singletonInstance == null) {
            singletonInstance = new ApiCommandImplementation();
        }
        return singletonInstance;
    }
}
