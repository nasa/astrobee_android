package gov.nasa.arc.irg.astrobee.guestscienceactivity;

import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by andres on 6/29/17.
 */

public class GuestScienceSampleApp {

    private static final String LOGTAG = "GS_SampleApp";
    private static RobotCommands robotCmds = new RobotCommands();
    public static ArrayList<Bundle> commandList = new ArrayList<Bundle>();
    private static int lastInstruction = -1;

    GuestScienceSampleApp(){
        setRobotInstructions();
    }

    private void setRobotInstructions() {
        commandList.add(robotCmds.stopMotion());
        //commandList.add(robotCmds.armPanAndTilt(45.0f, 0.0f, "Pan"));
        commandList.add(robotCmds.moveTo(1.0, 0.0, 0.0, 0.0, 0.0, 0.0));
    }

    public void executeRobotInstructions(String response) {

        if(lastInstruction < commandList.size()) {
            if (lastInstruction == -1){
                lastInstruction++;

                robotCmds.displayMsgGSToRobot(commandList.get(lastInstruction).getString("cmd"));
                robotCmds.sendMessageToService(commandList.get(lastInstruction));
            } else {

                if (response.contains("STATUS_COMPLETE_SUCCESS")) {
                    robotCmds.displayMsgGSToRobot(commandList.get(lastInstruction).getString("cmd"));
                    robotCmds.sendMessageToService(commandList.get(lastInstruction));
                    lastInstruction++;
                }
                if (response.contains("STATUS_FAILED_OTHER")) {
                    robotCmds.displayMsgInGUI(response);
                }
            }
        } else {
            robotCmds.displayMsgInGUI(response);
        }
    }
}
