
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

package gov.nasa.arc.irg.astrobee.android_ros_bridge;

import android.util.Log;

import org.apache.commons.lang.ObjectUtils;
import org.ros.message.Time;
import org.ros.time.*;
import org.ros.RosCore;
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

import ff_msgs.AckStatus;
import ff_msgs.AckStamped;
import ff_msgs.CommandArg;
import ff_msgs.CommandConstants;
import ff_msgs.CommandStamped;
import std_msgs.Header;

import static java.lang.Boolean.TRUE;

/**
 * Created by amora on 3/3/17.
 */

public class GuestScienceDemo extends AbstractNodeMain implements MessageListener<AckStamped> {
    private static final String TAG = "SimpleNode";

    @Override
    public void onNewMessage(ff_msgs.AckStamped ack) { // Callback
        OnMessageListener listener = mListener;
        if (listener == null) {
            return;
        }

        String msg = "Command " + ack.getCmdId();
        if (ack.getCompletedStatus().getStatus() == 0) {
            msg = msg + " is still executing!";
        } else if (ack.getCompletedStatus().getStatus() == 1) {
            msg = msg + " completed successfully!";

            if (getStatusMove() == FINISHED) {
                if (getMoveType() == STOPPED) {
                    msg = msg + " COMPLETED MISSION!";
                }
            } else if (getMoveType() == MOVE_STRAIGHT) {
                double[] targetPos = getTargetPosition();
                msg = msg + " moving forward! to x: " + targetPos[0] + ", y: " + targetPos[1] + ", z: " + targetPos[2];
            }
        } else if (ack.getCompletedStatus().getStatus() == 2) {
            msg = msg + " failed with bad syntax. Error msg: " + ack.getMessage();
        } else if (ack.getCompletedStatus().getStatus() == 3) {
            msg = msg + " failed with message: " + ack.getMessage();
        } else if (ack.getCompletedStatus().getStatus() == 4) {
            msg = msg + " was cancelled.";
        }

        try {
            listener.onMessage(msg);
        } catch (Exception ex) {
            Log.e(TAG, "OnMessageListener threw an exception", ex);
        }
    }

    interface OnMessageListener {
        void onMessage(final String msg);
    }

    private OnMessageListener mListener = null;
    private Publisher<CommandStamped> mPublisher;
    private NodeConfiguration mNodeConfig;
    private MessageFactory mMsgFac;

    private String cmdId = "";
    private double[] position = {0.0, 0.0, 0.0};
    private double currentAngle = 0.0;
    private double targetAngle = 0.0;

    private static final int FINISHED = 1;
    private static final int UNFINISHED = 0;
    private int statusMove = UNFINISHED;

    private static final int STOPPED = 0;
    private static final int MOVE_STRAIGHT = 1;
    private static final int MOVE_CIRCLE = 2;
    private static final int MOVE_SPIRAL = 3;
    private int moveType = STOPPED;

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

    public void setMoveType(int moveType) {
        this.moveType = moveType;
    }

    public double[] getTargetPosition() {
        return position;
    }

    public void setTargetPosition(double[] position) {
        this.position = position;
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


    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("gS_demo_node");
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

    public void sendMessage(String cmdToSend) {
        System.out.println("gsdemo1 " + cmdToSend);
        if (cmdToSend == ""){
            return;
        } else if (cmdToSend.equals("straight")){
            straightLineTest();
        }
    }

    public void straightLineTest(){
        System.out.println("gsdemo2");
        double [] position = {1.0, 1.0, 0.0};
        setMoveType(MOVE_STRAIGHT);
        setTargetPosition(position);
        move("gs_straightLine", position);
    }

    public void move(String cmdId, double[] pos) {
        if (mPublisher == null) {
            return;
        }
        System.out.println("gsdemo3");
        ff_msgs.CommandStamped move_cmd = mPublisher.newMessage();
        std_msgs.Header hdr = mMsgFac.newFromType(Header._TYPE);
        Time myTime = new Time();
        myTime.secs = 1487370000;
        myTime.nsecs = 0;
        hdr.setStamp(myTime);
        //hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        move_cmd.setHeader(hdr);
        // Command names listed in CommandConstants.h
        move_cmd.setCmdName(ff_msgs.CommandConstants.CMD_NAME_SIMPLE_MOVE6DOF);

        // Command id needs to be a unique id that you will use make sure the command
        // was executed, usually a combination of username and timestamp
        // move_cmd.setCmdId("guest_science_1");
        move_cmd.setCmdId(cmdId);

        // Source of the command, set to guest_science so that the system knows that
        // the command didn't come from the ground
        move_cmd.setCmdSrc("guest_science");

        ff_msgs.CommandArg cmdArg1 = mMsgFac.newFromType(CommandArg._TYPE);

        // can really set it to anything
        cmdArg1.setDataType(ff_msgs.CommandArg.DATA_TYPE_STRING);
        cmdArg1.setS("world");

        List<CommandArg> args = new ArrayList<CommandArg>();
        args.add(cmdArg1);


        ff_msgs.CommandArg cmdArg2 = mMsgFac.newFromType(CommandArg._TYPE);

        // Set location where you want Astrobee to go to
        cmdArg2.setDataType(ff_msgs.CommandArg.DATA_TYPE_VEC3d);

        // double [] pos = {1.0, 1.0, 0.0};
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

        move_cmd.setArgs(args);

        mPublisher.publish(move_cmd);

    }

    public void setListener(OnMessageListener listener) {
        mListener = listener;
    }
}

