
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

package gov.nasa.arc.astrobee.android.gs.manager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Message;
import android.util.Log;

import org.apache.commons.lang.ObjectUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ff_msgs.AckCompletedStatus;
import ff_msgs.AckStamped;
import ff_msgs.AckStatus;
import ff_msgs.CommandConstants;
import ff_msgs.CommandStamped;
import ff_msgs.GuestScienceApk;
import ff_msgs.GuestScienceCommand;
import ff_msgs.GuestScienceConfig;
import ff_msgs.GuestScienceData;
import ff_msgs.GuestScienceState;
import ff_msgs.Heartbeat;
import std_msgs.Header;

import static org.jboss.netty.buffer.ChannelBuffers.buffer;
import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

class ManagerNode extends AbstractNodeMain implements MessageListener<CommandStamped> {
    private static final String LOG_TAG = "GuestScienceManager";
    private static final String INFO_ACTION = "gov.nasa.arc.astrobee.android.gs.INFO_INQUIRY";

    private CmdInfo mCmdInfo;

    private Context mContext;

    private ManagerTimeoutTimer mStartTimer;

    private Logger mLogger;

    private Map<String, PendingIntent> mApkStartIntents;
    // Map used to store the location of the running state in the guest science manager state array
    // for each apk
    private Map<String, Integer> mApkStateLocations;

    private Publisher<AckStamped> mAckPublisher;
    private Publisher<GuestScienceConfig> mConfigPublisher;
    private Publisher<GuestScienceData> mDataPublisher;
    private Publisher<GuestScienceState> mStatePublisher;
    private Publisher<Heartbeat> mHeartbeatPublisher;

    private Subscriber<CommandStamped> mCommandSubscriber;

    private NodeConfiguration mNodeConfig;

    private MessageFactory mMessageFactory;

    private GuestScienceConfig mConfig;

    private GuestScienceState mState;

    private Heartbeat mHeartbeat;

    class PublishHeartbeat extends TimerTask {
        public void run() {
            sendHeartbeat();
        }
    }

    class RestartGuestScienceAPK extends TimerTask {
        public void run() {
            startGuestScienceAPK(null);
        }
    }
    /* Lazy initialization singleton pattern */
    private ManagerNode() { }

    private static class Holder {
        static final ManagerNode INSTANCE = new ManagerNode();
    }

    public static ManagerNode INSTANCE() {
        return Holder.INSTANCE;
    }

    public Logger getLogger() {
        return mLogger;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    @Override
    public void onNewMessage(CommandStamped cmd) {
        Header hdr = mMessageFactory.newFromType(Header._TYPE);

        String msg, apkName;

        // Command syntax checked in executive
        if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_CUSTOM_GUEST_SCIENCE)) {
            apkName = cmd.getArgs().get(0).getS();
            String command = cmd.getArgs().get(1).getS();
            if (MessengerService.getSingleton().sendGuestScienceCustomCommand(apkName, command)) {
                sendAck(cmd.getCmdId());
            } else {
                msg = "Couldn't send command " + command + " to apk " + apkName + ". More than " +
                        "likely the apk wasn't started.";
                sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
                mLogger.error(LOG_TAG, msg);
            }
        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_RESTART_GUEST_SCIENCE)) {
            if (stopGuestScienceAPK(cmd, CmdType.RESTART)) {
                // Apk was successfully stopped. Set a timer to give the apk a couple seconds
                // stopped and then try starting it.
                // Time between the stop and start is the 2nd parameter in the command
                long restartTimeMilliseconds = cmd.getArgs().get(1).getI() * 1000;
                mLogger.debug(LOG_TAG, "Restart timer set for " + cmd.getArgs().get(1).getI() + " seconds/" + restartTimeMilliseconds + " milliseconds.");
                Timer restartTimer = new Timer();
                restartTimer.schedule(new RestartGuestScienceAPK(), restartTimeMilliseconds);
            } else {
                // Stopped failed so the command information should be cleared in case it was set
                mCmdInfo.resetCmd();
            }
        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_STOP_GUEST_SCIENCE)) {
            if (stopGuestScienceAPK(cmd, CmdType.STOP)) {
                  sendAck(mCmdInfo.mId);
            }
            mCmdInfo.resetCmd();
        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_START_GUEST_SCIENCE)) {
            startGuestScienceAPK(cmd);
        } else {
            msg = "Command " + cmd.getCmdName() + " is not a guest science command.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            mLogger.error(LOG_TAG, msg);
        }
    }

    public boolean guestScienceStartStopCommandAlreadyRunning(String cmdId) {
        String msg;
        // Check to see if a start, stop, or restart guest science command is being executed. If it
        // is, don't execute the start, stop, or restart command we just received.
        if (!mCmdInfo.isCmdEmpty()) {
            msg = "The guest science manager is busy trying to " + mCmdInfo.getCmdType() + " a " +
                  "different apk. Please wait until the command completes and then try issuing " +
                  "the start/stop/restart command again!";
            sendAck(cmdId, AckCompletedStatus.EXEC_FAILED, msg);
            mLogger.error(LOG_TAG, msg);
            return true;
        }
        return false;
        
    }

    public void startGuestScienceAPK(CommandStamped cmd) {
        String apkName, cmdId, msg;

        mLogger.debug(LOG_TAG, "Attempting to start guest science apk.");

        // If we are in the middle of a restart, cmd will be null
        if (cmd == null) {
            apkName = mCmdInfo.mApkName;
            cmdId = mCmdInfo.mId;
        } else {
            if (guestScienceStartStopCommandAlreadyRunning(cmd.getCmdId())) {
                return;
            }

            apkName = cmd.getArgs().get(0).getS();
            cmdId = cmd.getCmdId();
            // Check if the apk is already started
            if (checkGuestScienceAPKAlreadyStartedStopped(CmdType.START, apkName, cmdId) != CmdStatus.NOTDONE) {
                return;
            }
        }

        if (mApkStartIntents.containsKey(apkName)) {
            PendingIntent startApkIntent = mApkStartIntents.get(apkName);
            try {
                startApkIntent.send();
                if (cmd != null) {
                    mCmdInfo.setCmd(cmdId, cmd.getCmdOrigin(), apkName, CmdType.START);
                }
                // Start timeout timer
                mStartTimer.start();
            } catch (PendingIntent.CanceledException e) {
                msg = "Guest sciencen manager encountered a pending intent canceled exeception " +
                      "when trying to start apk " + apkName + ".";
                sendAck(cmdId, AckCompletedStatus.EXEC_FAILED, msg);
                mLogger.error(LOG_TAG, msg, e);
                return;
            }
        } else {
            msg = "Got command to start/restart " + apkName + " but gs manager didn't receive a " +
                  "valid information bundle and thus doesn't have the pending intent needed to " +
                  "start the apk.";
            sendAck(cmdId, AckCompletedStatus.EXEC_FAILED, msg);
            mLogger.error(LOG_TAG, msg);
            return;
        }
        return;
    }

    public boolean stopGuestScienceAPK(CommandStamped cmd, CmdType cmdTypeIn) {
        String msg, apkName;

        mLogger.debug(LOG_TAG, "Attempting to stop guest science apk.");

        if (guestScienceStartStopCommandAlreadyRunning(cmd.getCmdId())) {
            return false;
        }

        apkName = cmd.getArgs().get(0).getS();

        // Check if apk is already stopped. If it is and the gs manager got a stop command, return
        // false so the gs manager doesn't double ack it. Otherwise, return true so the restart will
        // continue to start the apk.
        CmdStatus cmdStatus = checkGuestScienceAPKAlreadyStartedStopped(cmdTypeIn,
                                                                        apkName,
                                                                        cmd.getCmdId());
        if (cmdStatus != CmdStatus.NOTDONE) {
            if (cmdTypeIn == CmdType.RESTART && cmdStatus == CmdStatus.PARTIAL) {
                mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), apkName, cmdTypeIn);
                return true;
            } else {
                return false;
            }
        }

        if (!MessengerService.getSingleton().sendGuestScienceStop(apkName)) {
            msg = "Couldn't send stop/restart command to apk " + apkName + ". More than likely " +
                  "the apk wasn't started.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            mLogger.error(LOG_TAG, msg);
            return false;
        }

        mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), apkName, cmdTypeIn);

        // TODO(Katie) Change this to happen after we receive confirmation that the messenger died
        // TODO(Katie) This will require code to detect that the messenger died

        // Ack successful in the calling function because if this is a restart apk command, we
        // don't want to ack until the start is successful
        return changeGuestScienceState(false, apkName);
    }

    public CmdStatus checkGuestScienceAPKAlreadyStartedStopped(CmdType cmdTypeIn,
                                                             String apkName,
                                                             String cmdId) {
        String ackMsg;
        if (!mApkStateLocations.containsKey(apkName)) {
            ackMsg = "Couldn't find the guest science apk in the guest science manager state." +
                     "More than likely, the guest science maanaager didn't receive a valid " +
                     "information bundle and thus can't start/stop the apk.";
            mLogger.error(LOG_TAG, ackMsg);
            sendAck(cmdId, AckCompletedStatus.EXEC_FAILED, ackMsg);
            return CmdStatus.ERROR;
        }

        int index = mApkStateLocations.get(apkName);
        boolean[] runningApks = mState.getRunningApks();
        boolean apkStarted = runningApks[index];

        // Check if trying to start an already started apk
        if (apkStarted && cmdTypeIn == CmdType.START) {
            ackMsg = apkName + " should be already started.";
            mLogger.debug(LOG_TAG, ackMsg);
            sendAck(cmdId, AckCompletedStatus.OK, ackMsg);
            return CmdStatus.DONE;
        } else if (!apkStarted && cmdTypeIn == CmdType.STOP) {
            // Check if trying to stop an already stopped apk
            ackMsg = apkName + " should already be stopped.";
            mLogger.debug(LOG_TAG, ackMsg);
            sendAck(cmdId, AckCompletedStatus.OK, ackMsg);
            return CmdStatus.DONE;
        } else if (!apkStarted && cmdTypeIn == CmdType.RESTART) {
            // Check if apk stopped but we got a restart command so we only need to start it
            ackMsg = "Restart apk command received but " + apkName + " should be stopped." +
                     " So the gs manager will start it.";
            mLogger.debug(LOG_TAG, ackMsg);
            // Send ack that command is not commplete
            sendAck(cmdId, AckCompletedStatus.NOT, ackMsg);
            return CmdStatus.PARTIAL;
        }
        return CmdStatus.NOTDONE;
    }

    public boolean changeGuestScienceState(boolean started, String apkName) {
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        String errMsg;
        if (!apkName.contentEquals(mCmdInfo.mApkName)) {
            // If the apks don't match, don't ack the command or change the gs manager state
            // since the apk probably started after the start timeout or the wrong apk started
            mLogger.error(LOG_TAG, "The name of the apk started doesn't match the current command" +
                                   " apk name. Apk " + apkName + " probably didn't start within" +
                                   " the start/stop timeout or the apk didn't send the full apk" +
                                   " name known to the guest science manager.");
            return false;
        }

        if (!mApkStateLocations.containsKey(apkName)) {
            errMsg = "Couldn't update the guest science manager state because it couldn't " +
                     "find the index for " + apkName + ". However the apk seemed to start/stop " +
                     "successfully.";
            mLogger.error(LOG_TAG, errMsg);
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
            return false;
        } else {
            int index = mApkStateLocations.get(apkName);
            boolean[] runningApks = mState.getRunningApks();
            runningApks[index] = started;
            mState.setRunningApks(runningApks);
            hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
            mState.setHeader(hdr);
            mStatePublisher.publish(mState);
        }
        return true;
    }

    public void ackGuestScienceStart(boolean startSuccessful, String apkName, String errMsg) {
        // Stop timeout timer
        mStartTimer.cancel();

        // If the apk started successfully, update the gs manager state and ack the start command
        if (startSuccessful) {
            if (changeGuestScienceState(true, apkName)) {
                sendAck(mCmdInfo.mId);
            }
        } else {
            // The apk didn't start successfully so don't update the state and fail the command ack
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
        }
        mCmdInfo.resetCmd();
    }

    public void onGuestScienceData(Message msg) {
        String apkFullName = "", topic = "";
        // Check to make sure the message contains the full name of the guest science apk
        if (!msg.getData().containsKey("apkFullName")) {
            mLogger.error(LOG_TAG, "Guest science message doesn't contain the full apk name and " +
                    "will not be processed!");
            return;
        }

        apkFullName = msg.getData().getString("apkFullName");

        if (msg.getData().containsKey("topic")) {
            topic = msg.getData().getString("topic");
            if (topic.length() > 32) {
                mLogger.error(LOG_TAG, "The topic string in the guest science message is too " +
                        "big to send to the ground so the message will not be sent. Length must " +
                        " be no more than 32 characters not " + topic.length() + ".");
                return;
            }
        }

        byte[] data = null;
        if (!msg.getData().containsKey("data")) {
            mLogger.error(LOG_TAG, "Guest science message doesn't contain data and will not be " +
                    "processed.");
            return;
        } else {
            data = msg.getData().getByteArray("data");
            if (data.length > 2048) {
                mLogger.error(LOG_TAG, "The data in the guest science message is too big to send " +
                        "to the ground so the message will not be sent. Length of data must be no" +
                        " more than 2048 bytes not " + data.length + ".");
                return;
            }
        }

        GuestScienceData dataMsg = mMessageFactory.newFromType(GuestScienceData._TYPE);
        Header hdr = mMessageFactory.newFromType(Header._TYPE);

        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        dataMsg.setHeader(hdr);

        dataMsg.setApkName(apkFullName);

        if (msg.what == MessageType.STRING.toInt()) {
            dataMsg.setDataType(GuestScienceData.STRING);
        } else if (msg.what == MessageType.JSON.toInt()) {
            dataMsg.setDataType(GuestScienceData.JSON);
        } else if (msg.what == MessageType.BINARY.toInt()) {
            dataMsg.setDataType(GuestScienceData.BINARY);
        } else {
            mLogger.error(LOG_TAG, "Message type in guest science message is unknown so the message " +
                    "will not be sent to the ground.");
            return;
        }

        dataMsg.setTopic(topic);

        // If there isn't data, don't copy it over as it will crash
        if (data.length != 0) {
            ChannelBuffer dataBuff = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, data);
            dataMsg.setData(dataBuff);
        }
        mDataPublisher.publish(dataMsg);
   }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("guest_science_manager");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Needs three publishers, one to ack guest science commands, one for
        // the guest science state, and one for guest science data
        mAckPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_ACK, AckStamped._TYPE);

        mConfigPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_CONFIG,
                GuestScienceConfig._TYPE);

        mDataPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_DATA, GuestScienceData._TYPE);

        mStatePublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_STATE,
                GuestScienceState._TYPE);

        mHeartbeatPublisher = connectedNode.newPublisher(Constants.TOPIC_HEARTBEAT,
                                                         Heartbeat._TYPE);

        // State, config, and heartbeat file topics are latched
        mConfigPublisher.setLatchMode(true);
        mStatePublisher.setLatchMode(true);
        mHeartbeatPublisher.setLatchMode(true);

        mCommandSubscriber = connectedNode.newSubscriber(
                Constants.TOPIC_MANAGEMENT_EXEC_COMMAND, CommandStamped._TYPE);
        mCommandSubscriber.addMessageListener(this);

        mNodeConfig = NodeConfiguration.newPrivate();
        mMessageFactory = mNodeConfig.getTopicMessageFactory();
        mConfig = mConfigPublisher.newMessage();
        mState = mStatePublisher.newMessage();
        mHeartbeat = mHeartbeatPublisher.newMessage();
        mHeartbeat.setNode("guest_science_manager");

        mApkStartIntents = new HashMap<>();
        mApkStateLocations = new HashMap<>();

        mLogger = new Logger(connectedNode.getLog());

        getApkInfo();

        if (mContext == null) {
            mLogger.error(LOG_TAG,
                    "Context is null so the gs manager cannot start the messenger service.");
            return;
        }
        // Start messenger service so there is only one instance running at all times
        Intent startMessengerIntent = new Intent(mContext, MessengerService.class);
        mContext.startService(startMessengerIntent);

        mCmdInfo = new CmdInfo();

        // Start heartbeat timer, heartbeat will be publish every second
        Timer timer = new Timer();
        timer.schedule(new PublishHeartbeat(), 0, 1000);
    }

    public void setStartTimer(ManagerTimeoutTimer startTimer) {
        mStartTimer = startTimer;
    }

    public String getCurrentApkName() {
       return mCmdInfo.mApkName;
    }

    public void getApkInfo() {
        Header hdr = mConfig.getHeader();
        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        mConfig.setHeader(hdr);
        // Serial number needs to match in the config and state messages so the ground data system
        // knows which state messages corresponds to the config message.
        mConfig.setSerial(1);

        Intent apkInfoIntent = new Intent(INFO_ACTION);
        apkInfoIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        if (mContext == null) {
            mLogger.error(LOG_TAG,
                    "Context is null so the gs manager cannot send the ordered broadcast.");
            return;
        }

        // Send ordered broadcast receiver to query apks for their information
        mContext.sendOrderedBroadcast(apkInfoIntent, null, new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Get apks message so we can add apk information to the message
               List<GuestScienceApk> apks = mConfig.getApks();

                // Need to extract guest science apk information from results bundle
                Bundle results = getResultExtras(true);
                // The keys are the guest science apk full names so we need to get an array of the
                // keys so we can extract the guest science apk info bundles out of the results
                // bundle.
                Set<String> keys = results.keySet();
                Object[] keysArray = keys.toArray();
                int stateArrayIndex = 0;
                for (int i = 0; i < keysArray.length; i++) {
                    String fullName = keysArray[i].toString();
                    Bundle apkInfo = results.getBundle(fullName);
                    // If the guest science apk doesn't send all the information, too bad for them.
                    // We can't send their info to the ground if we don't have all of it.
                    if (!apkInfo.containsKey("shortName") ||
                            !apkInfo.containsKey("primary") ||
                            !apkInfo.containsKey("startIntent")) {
                        continue;
                    }

                    String shortName = apkInfo.getString("shortName");
                    boolean primary = apkInfo.getBoolean("primary");
                    PendingIntent startIntent = apkInfo.getParcelable("startIntent");

                    // If the start intent is null, we cannot start the apk so we can't send
                    // the apk info to the ground
                    if (startIntent == null) {
                        mLogger.warn(LOG_TAG, "Start intent is null for apk " + fullName);
                        continue;
                    }

                    // Make an apk object to put in config message
                    GuestScienceApk apk = mMessageFactory.newFromType(GuestScienceApk._TYPE);
                    apk.setApkName(fullName);
                    apk.setShortName(shortName);
                    apk.setPrimary(primary);

                    // Put pending intent in a map that we can use later to start the apk
                    mApkStartIntents.put(fullName, startIntent);

                    // Put location of guest science apk in the manager state in a map so that we
                    // don't have to search the manager config every time we want to mark an apk as
                    // started or stopped in the manager state
                    mApkStateLocations.put(fullName, stateArrayIndex);

                    // Check if the guest science apk has custom commands and if so, add them to the
                    // config so the ground knows about them
                    if (apkInfo.containsKey("commands")) {
                        List<GuestScienceCommand> cmds = apk.getCommands();
                        ArrayList<String> commands = apkInfo.getStringArrayList("commands");
                        for (int j = 0; j < commands.size(); j++) {
                            String commandName = commands.get(j);
                            if (!apkInfo.containsKey(commandName)) {
                                continue;
                            }
                            String commandSyntax = apkInfo.getString(commandName);
                            GuestScienceCommand cmd =
                                    mMessageFactory.newFromType(GuestScienceCommand._TYPE);
                            cmd.setName(commandName);
                            cmd.setCommand(commandSyntax);
                            cmds.add(cmd);
                        }
                        apk.setCommands(cmds);
                    }
                    apks.add(apk);
                    stateArrayIndex++;
                }
                // Add apks to config and publish the config message
                mConfig.setApks(apks);
                mConfigPublisher.publish(mConfig);

                // Initialize the state message
                boolean[] runningApks = new boolean[mConfig.getApks().size()];
                Arrays.fill(runningApks, false);

                Header stateHeader = mState.getHeader();
                stateHeader.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());

                mState.setHeader(stateHeader);
                mState.setSerial(mConfig.getSerial());
                mState.setRunningApks(runningApks);
                mStatePublisher.publish(mState);
            }
        }, null, Activity.RESULT_OK, null, null);
    }

    public void sendAck(String cmdId) {
        sendAck(cmdId, AckCompletedStatus.OK, "", AckStatus.COMPLETED);
    }

    public void sendAck(String cmdId, byte completedStatus, String message) {
        sendAck(cmdId, completedStatus, message, AckStatus.COMPLETED);
    }

    public void sendAck(String cmdId, byte completedStatus, String message,
                        byte status) {
        if (mAckPublisher == null) {
            return;
        }

        AckStamped ack = mAckPublisher.newMessage();
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        ack.setHeader(hdr);

        ack.setCmdId(cmdId);

        AckCompletedStatus ackCS = mMessageFactory.newFromType(AckCompletedStatus._TYPE);
        ackCS.setStatus(completedStatus);
        ack.setCompletedStatus(ackCS);

        ack.setMessage(message);

        AckStatus ackStatus = mMessageFactory.newFromType(AckStatus._TYPE);
        ackStatus.setStatus(status);
        ack.setStatus(ackStatus);

        mAckPublisher.publish(ack);
    }

    public void sendHeartbeat() {
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        mHeartbeat.setHeader(hdr);

        mHeartbeatPublisher.publish(mHeartbeat);
    }
}
