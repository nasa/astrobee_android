package gov.nasa.arc.irg.astrobee.android_ros_bridge;

import android.util.Log;

import org.ros.message.Time;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;
import java.util.List;

import ff_msgs.AckStamped;
import ff_msgs.CommandArg;
import ff_msgs.CommandConstants;
import ff_msgs.CommandStamped;
import std_msgs.Header;

/**
 * Created by amora on 3/3/17.
 */

public class GuestScienceRosMessages extends AbstractNodeMain implements MessageListener<AckStamped> {

    //To create an instance of this singleton class
    private static GuestScienceRosMessages singletonInstance;

    private static final String LOGTAG = "SimpleNode";

    private OnMessageListener mListener = null;
    private static Publisher<CommandStamped> mPublisher;
    private NodeConfiguration mNodeConfig;
    private static MessageFactory mMsgFac;

    private String cmdId = "";
    private static double[] position = {0.0, 0.0, 0.0};
    private double currentAngle = 0.0;
    private double targetAngle = 0.0;

    private static final int FINISHED = 1;
    private static final int UNFINISHED = 0;
    private int statusMove = UNFINISHED;

    private static final int STOPPED = 0;
    private static final int MOVE_STRAIGHT = 1;
    private static final int MOVE_CIRCLE = 2;
    private static final int MOVE_SPIRAL = 3;
    private static int moveType = STOPPED;

    private final int INT_STILL_EXECUTING = 0;
    private final int INT_COMPLETE_SUCCESS = 1;
    private final int INT_BAD_SYNTAX = 2;
    private final int INT_FAILED_OTHER = 3;
    private final int INT_CANCELLED = 4;

    private final String STR_STILL_EXECUTING = " STATUS_STILL_EXECUTING ";
    private final String STR_COMPLETE_SUCCESS = " STATUS_COMPLETE_SUCCESS ";
    private final String STR_BAD_SYNTAX = " STATUS_BAD_SYNTAX ";
    private final String STR_FAILED_OTHER = " STATUS_FAILED_OTHER ";
    private final String STR_CANCELLED = " STATUS_CANCELLED ";

    /* A private Constructor prevents any other class from instantiating. */
    private GuestScienceRosMessages() {}

    @Override
    public void onNewMessage(ff_msgs.AckStamped ack) { // Callback
        OnMessageListener listener = mListener;
        if (listener == null) {
            return;
        }

        String msg = "Command " + ack.getCmdId();
        if (ack.getCompletedStatus().getStatus() == INT_STILL_EXECUTING) {
            msg = msg + STR_STILL_EXECUTING;
        } else if (ack.getCompletedStatus().getStatus() == INT_COMPLETE_SUCCESS) {
            msg = msg + STR_COMPLETE_SUCCESS;
        } else if (ack.getCompletedStatus().getStatus() == INT_BAD_SYNTAX) {
            msg = msg + STR_BAD_SYNTAX + "Error msg: " + ack.getMessage();
        } else if (ack.getCompletedStatus().getStatus() == INT_FAILED_OTHER) {
            msg = msg + STR_FAILED_OTHER + "failed with message: " + ack.getMessage();
        } else if (ack.getCompletedStatus().getStatus() == INT_CANCELLED) {
            msg = msg + STR_CANCELLED;
        }

        try {
            listener.onMessage(msg);
        } catch (Exception ex) {
            Log.e(LOGTAG, "OnMessageListener threw an exception", ex);
        }
    }

    interface OnMessageListener {
        void onMessage(final String msg);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("gs_demo_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Publisher<ff_msgs.CommandStamped> publisher = connectedNode.newPublisher(
                "command", ff_msgs.CommandStamped._TYPE);
        mPublisher = publisher;

        Subscriber<AckStamped> subscriber = connectedNode.newSubscriber(
                "mgt/ack", ff_msgs.AckStamped._TYPE);
        subscriber.addMessageListener(this);

        mNodeConfig = NodeConfiguration.newPrivate();
        mMsgFac = mNodeConfig.getTopicMessageFactory();
    }

    public void stopAllMotion() {
        if (mPublisher == null) {
            return;
        }

        ff_msgs.CommandStamped cmdToSend = mPublisher.newMessage();
        std_msgs.Header hdr = mMsgFac.newFromType(Header._TYPE);
        Time myTime = new Time();
        myTime.secs = 1487370000;
        myTime.nsecs = 0;
        hdr.setStamp(myTime);
        cmdToSend.setHeader(hdr);
        // Command names listed in CommandConstants.h
        cmdToSend.setCmdName(ff_msgs.CommandConstants.CMD_NAME_STOP_ALL_MOTION);

        // Command id needs to be a unique id that you will use make sure the command
        // was executed, usually a combination of username and timestamp
        // move_cmd.setCmdId("guest_science_1");
        cmdToSend.setCmdId("guest_science_" + ff_msgs.CommandConstants.CMD_NAME_STOP_ALL_MOTION);

        // Source of the command, set to guest_science so that the system knows that
        // the command didn't come from the ground
        cmdToSend.setCmdSrc("guest_science");

        ff_msgs.CommandArg cmdArg1 = mMsgFac.newFromType(CommandArg._TYPE);

        // can really set it to anything
        cmdArg1.setDataType(ff_msgs.CommandArg.DATA_TYPE_STRING);
        cmdArg1.setS("world");

        List<CommandArg> args = new ArrayList<CommandArg>();
        args.add(cmdArg1);

        cmdToSend.setArgs(args);

        mPublisher.publish(cmdToSend);
    }

    public void armPanAndTilt(float pan, float tilt, String componentToMove) {
        if (mPublisher == null) {
            return;
        }

        ff_msgs.CommandStamped cmdToSend = mPublisher.newMessage();
        std_msgs.Header hdr = mMsgFac.newFromType(Header._TYPE);
        Time myTime = new Time();
        myTime.secs = 1487370000;
        myTime.nsecs = 0;
        hdr.setStamp(myTime);
        cmdToSend.setHeader(hdr);
        // Command names listed in CommandConstants.h
        cmdToSend.setCmdName(ff_msgs.CommandConstants.CMD_NAME_ARM_PAN_AND_TILT);

        // Command id needs to be a unique id that you will use make sure the command
        // was executed, usually a combination of username and timestamp
        cmdToSend.setCmdId("guest_science_" + ff_msgs.CommandConstants.CMD_NAME_ARM_PAN_AND_TILT);

        // Source of the command, set to guest_science so that the system knows that
        // the command didn't come from the ground
        cmdToSend.setCmdSrc("guest_science");

        // Set the ARM component to move
        ff_msgs.CommandArg cmdArg1 = mMsgFac.newFromType(CommandArg._TYPE);
        cmdArg1.setDataType(ff_msgs.CommandArg.DATA_TYPE_FLOAT);
        cmdArg1.setF(pan);

        // Set the ARM angle1 to move
        ff_msgs.CommandArg cmdArg2 = mMsgFac.newFromType(CommandArg._TYPE);
        cmdArg2.setDataType(CommandArg.DATA_TYPE_FLOAT);
        cmdArg2.setF(tilt);

        // Set the ARM angle2 to move
        ff_msgs.CommandArg cmdArg3 = mMsgFac.newFromType(CommandArg._TYPE);
        cmdArg3.setDataType(CommandArg.DATA_TYPE_STRING);
        cmdArg3.setS(componentToMove);

        List<CommandArg> args = new ArrayList<CommandArg>();
        args.add(cmdArg1);
        args.add(cmdArg2);
        args.add(cmdArg3);

        cmdToSend.setArgs(args);

        mPublisher.publish(cmdToSend);

    }

    public void move(String cmdId, double[] pos) {
        if (mPublisher == null) {
            return;
        }

        ff_msgs.CommandStamped cmdToSend = mPublisher.newMessage();
        std_msgs.Header hdr = mMsgFac.newFromType(Header._TYPE);
        Time myTime = new Time();
        myTime.secs = 1487370000;
        myTime.nsecs = 0;
        hdr.setStamp(myTime);
        cmdToSend.setHeader(hdr);
        // Command names listed in CommandConstants.h
        cmdToSend.setCmdName(ff_msgs.CommandConstants.CMD_NAME_SIMPLE_MOVE6DOF);

        // Command id needs to be a unique id that you will use make sure the command
        // was executed, usually a combination of username and timestamp
        cmdToSend.setCmdId("guest_science_" + cmdId);

        // Source of the command, set to guest_science so that the system knows that
        // the command didn't come from the ground
        cmdToSend.setCmdSrc("guest_science");

        ff_msgs.CommandArg cmdArg1 = mMsgFac.newFromType(CommandArg._TYPE);

        // can really set it to anything
        cmdArg1.setDataType(ff_msgs.CommandArg.DATA_TYPE_STRING);
        cmdArg1.setS("world");

        List<CommandArg> args = new ArrayList<CommandArg>();
        args.add(cmdArg1);

        ff_msgs.CommandArg cmdArg2 = mMsgFac.newFromType(CommandArg._TYPE);
        // Set location where you want Astrobee to go to
        cmdArg2.setDataType(ff_msgs.CommandArg.DATA_TYPE_VEC3d);

        cmdArg2.setVec3d(pos);

        ff_msgs.CommandArg cmdArg3 = mMsgFac.newFromType(CommandArg._TYPE);
        // Set location where you want Astrobee to go to
        cmdArg3.setDataType(ff_msgs.CommandArg.DATA_TYPE_VEC3d);

        double [] tol = {0.0, 0.0, 0.0};
        cmdArg3.setVec3d(tol);

        ff_msgs.CommandArg cmdArg4 = mMsgFac.newFromType(CommandArg._TYPE);
        // Set location where you want Astrobee to go to
        cmdArg4.setDataType(ff_msgs.CommandArg.DATA_TYPE_MAT33f);

        float [] qt = {0, 0, 0, 1, 0, 0, 0, 0, 0};
        cmdArg4.setMat33f(qt);

        args.add(cmdArg2);
        args.add(cmdArg3);
        args.add(cmdArg4);

        cmdToSend.setArgs(args);

        mPublisher.publish(cmdToSend);

    }

    public static GuestScienceRosMessages getSingletonInstance() {
        if(singletonInstance == null) {
            singletonInstance = new GuestScienceRosMessages();
        }
        return singletonInstance;
    }

    public void setListener(OnMessageListener listener) {
        mListener = listener;
    }

    public String getCmdId() {
        return cmdId;
    }

    public void setCmdId(String cmdId) {
        this.cmdId = cmdId;
    }

    public int getStatusMove() {
        return statusMove;
    }

    public void setStatusMove(int statusMove) {
        this.statusMove = statusMove;
    }

    public int getMoveType() {
        return moveType;
    }

    private static void setMoveType(int moveType) {
        GuestScienceRosMessages.moveType = moveType;
    }

    public double[] getTargetPosition() {
        return position;
    }

    private static void setTargetPosition(double[] position) {
        GuestScienceRosMessages.position = position;
    }

    public double getCurrentAngle() {
        return currentAngle;
    }

    public void setCurrentAngle(double currentAngle) {
        this.currentAngle = currentAngle;
    }

    public double getTargetAngle() {
        return targetAngle;
    }

    public void setTargetAngle(double targetAngle) {
        this.targetAngle = targetAngle;
    }
}

/*private void straightLineTest(){
        Log.i(LOGTAG, "gsdemo2");
        double [] position = {1.0, 1.0, 0.0};
        setMoveType(MOVE_STRAIGHT);
        setTargetPosition(position);
        move("gs_straightLine", position);
    }*/

/*public void sendMessage(String cmdToSend) {
        Log.i(LOGTAG, "gsdemo1 " + cmdToSend);
        if (cmdToSend == ""){
            return;
        } else if (cmdToSend.equals("straight")){
            straightLineTest();
        }
    }*/

