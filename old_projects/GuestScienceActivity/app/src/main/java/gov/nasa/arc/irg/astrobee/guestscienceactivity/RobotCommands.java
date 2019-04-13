package gov.nasa.arc.irg.astrobee.guestscienceactivity;

import android.os.Bundle;

/**
 * Created by Andres Mora on 6/22/17.
 */

public class RobotCommands {

    private final String LOGTAG = "RobotCommands";

    private String CMD_NAME_ARM_PAN_AND_TILT = "armPanAndTilt";
    private String CMD_NAME_SIMPLE_MOVE6DOF = "simpleMove6DOF";
    private String CMD_NAME_STOP_ALL_MOTION = "stopAllMotion";

    RobotCommands() {    }

    /**
     * Creates a bundle with the data for the stopMotion command
     * @return bundle of stopMotion command data
     */
    public Bundle stopMotion () {
        Bundle bundle = new Bundle();
        String cmd = CMD_NAME_STOP_ALL_MOTION;
        int numArgs = 0;
        bundle.putString("cmd", cmd);
        bundle.putInt("numArgs", numArgs);
        return bundle;
    }

    /**
     * Creates a bundle with the data for the armPanAndTilt command
     * @param componentToMove sets the element of the arm that will move
     * @param pan Angle [degrees] first component of the arm will move
     * @param tilt Angle [degrees] second component of the arm will move
     * @return bundle of armPanAndTilt command data
     */
    public Bundle armPanAndTilt (float pan, float tilt, String componentToMove){
        Bundle bundle = new Bundle();
        String cmd = CMD_NAME_ARM_PAN_AND_TILT;
        int numArgs = 3;
        String cmpToMove = componentToMove;
        bundle.putString("cmd", cmd);
        bundle.putInt("numArgs", numArgs);
        bundle.putString("cmpToMove", cmpToMove);
        bundle.putFloat("angle1", pan);
        bundle.putFloat("angle2", tilt);
        return bundle;
    }

    /**
     * Creates a bundle with the data for the move command
     * @param x coordinates on the x axis
     * @param y coordinates on the y axis
     * @param z coordinates on the z axis
     * @param phi roll attitude angle
     * @param theta pitch attitude angle
     * @param gamma yaw attitude angle
     * @return bundle of move command data
     */
    public Bundle moveTo(double x, double y, double z, double phi, double theta, double gamma){
        //For now only passing the xyz position. Keeping tolerances and quaternions unchanged.
        Bundle bundle = new Bundle();
        String cmd = CMD_NAME_SIMPLE_MOVE6DOF;
        int numArgs = 6;
        double [] pos = new double[3];
        double [] att = new double[3];
        pos[0] = x;
        pos[1] = y;
        pos[2] = z;
        att[0] = phi;
        att[1] = theta;
        att[2] = gamma;
        bundle.putString("cmd", cmd);
        bundle.putInt("numArgs", numArgs);
        bundle.putDoubleArray("pos", pos);
        bundle.putDoubleArray("att", att);

        return bundle;
    }

    /**
     * Helps interface the method in MainActivity with the GuestScience Application class
     * @param bundleToSend bundle of data of each command, e.g. stopMotion, moveTo, etc.
     */
    public void sendMessageToService(Bundle bundleToSend) {
        MainActivity.sendMessageToService(bundleToSend);
    }

    public void displayMsgInGUI(String msgRxd){
        MainActivity.setmTextMsgServiceToRobot(msgRxd);
    }

    public void displayMsgGSToRobot(String msgRxd) {
        MainActivity.setmTextMsgGSToRobot(msgRxd);
    }
}
