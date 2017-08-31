
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

package gov.nasa.arc.irg.astrobee.guestsciencemanager;

import android.util.Log;

import com.google.common.base.Ascii;

import org.jboss.netty.buffer.ChannelBuffer;
import org.ros.internal.message.field.ChannelBufferField;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ff_msgs.AckCompletedStatus;
import ff_msgs.AckStamped;
import ff_msgs.AckStatus;
import ff_msgs.CommandArg;
import ff_msgs.CommandConstants;
import ff_msgs.CommandStamped;
import ff_msgs.GuestScienceApk;
import ff_msgs.GuestScienceCommand;
import ff_msgs.GuestScienceConfig;
import ff_msgs.GuestScienceData;
import ff_msgs.GuestScienceState;
import std_msgs.Header;

import static com.google.common.base.Ascii.US;
import static com.google.common.base.CharMatcher.ASCII;
import static org.jboss.netty.buffer.ChannelBuffers.buffer;
import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

class ManagerNode extends AbstractNodeMain implements MessageListener<CommandStamped> {
    private static final String TAG = "GuestScienceManagerNode";
    private OnMessageListener listener_ = null;
    private Publisher<AckStamped> ack_publisher_;
    private Publisher<GuestScienceConfig> config_publisher_;
    private Publisher<GuestScienceData> data_publisher_;
    private Publisher<GuestScienceState> state_publisher_;
    private Subscriber<CommandStamped> command_subscriber_;
    private NodeConfiguration node_config_;
    private MessageFactory message_factory_;
    private GuestScienceConfig config_;
    private GuestScienceData rf_data_;
    private GuestScienceState state_;
    private String apk_name1_, apk_name2_;
    private Boolean internal_;

    /* Lazy initialization singleton pattern */
    private ManagerNode() { }

    private static class Holder {
        static final ManagerNode INSTANCE = new ManagerNode();
    }

    public static ManagerNode INSTANCE() {
        return Holder.INSTANCE;
    }

    @Override
    public void onNewMessage(CommandStamped command) {
        Header hdr = message_factory_.newFromType(Header._TYPE);
        OnMessageListener listener = listener_;
        if (listener == null) {
            return;
        }

        String out_msg = "Got command " + command.getCmdName() + " with id " + command.getCmdId();

        String msg;

        GuestScienceData data = message_factory_.newFromType(GuestScienceData._TYPE);
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
        data.setHeader(hdr);
        data.setDataType(GuestScienceData.JSON);
        data.setTopic("Astrobee.MessageType");

        if (command.getCmdSrc().equals("plan")) {
            // Set internal to true so the executive knows to ack the plan item and not send the
            // ack to the ground
            internal_ = true;
        } else {
            internal_ = false;
        }

        // Command syntax checked in executive
        // TODO(Katie) Replace with actual code
        if (command.getCmdName().equals(CommandConstants.CMD_NAME_CUSTOM_GUEST_SCIENCE)) {
            if (command.getArgs().get(0).getS().equals(apk_name1_)) { // air_sampler
                if (command.getArgs().get(1).getS().equals("{name=On}")) {
                    sendAck(command.getCmdId());
                } else if (command.getArgs().get(1).getS().equals("{name=Off}")) {
                    sendAck(command.getCmdId());
                } else if (command.getArgs().get(1).getS().equals("{name=Take, num=5, time_between=10}")) {
                    sendAck(command.getCmdId(), AckCompletedStatus.NOT, "", AckStatus.EXECUTING);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    data.setApkName(apk_name1_);
                    String s_data = "{\"Summary\": \"sample 1: 26.8135\"}";
                    ChannelBuffer buff = data.getData();
                    buff.setBytes(0, s_data.getBytes());
                    buff.setIndex(0, s_data.length());
                    data.setData(buff);
                    data_publisher_.publish(data);
                    sendAck(command.getCmdId());
                } else {
                    msg = "Apk " + apk_name1_ + " doesn't recognize command " +
                            command.getArgs().get(1).getS();
                    sendAck(command.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
                    return;
                }

            } else if (command.getArgs().get(0).getS().equals(apk_name2_)) { // rfid_reader
                if (command.getArgs().get(1).getS().equals("{name=Send}")) {
                    hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
                    rf_data_.setHeader(hdr);
                    data_publisher_.publish(rf_data_);
                    sendAck(command.getCmdId());
                } else if (command.getArgs().get(1).getS().equals("{name=Remove, item=SciCamera}")) {
                    hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
                    rf_data_.setHeader(hdr);
                    String data_rf = "{\"Summary\": \"HazCamera, NavCamera, DockCamera, PerchCamera, Laser, FrontFlashlight, BackFlashlight\"}";
                    ChannelBuffer buff = rf_data_.getData();
                    buff.setBytes(0, data_rf.getBytes());
                    buff.setIndex(0, data_rf.length());
                    rf_data_.setData(buff);
                    data_publisher_.publish(rf_data_);
                    sendAck(command.getCmdId());
                } else {
                    msg = "Apk " + apk_name2_ + " doesn't recognize command " +
                            command.getArgs().get(1).getS();
                    sendAck(command.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
                    return;
                }
            } else {
                msg = "Apk " + command.getArgs().get(0).getS() + " not recognized.";
                sendAck(command.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
                return;
            }
        } else if (command.getCmdName().equals(CommandConstants.CMD_NAME_START_GUEST_SCIENCE) ||
                   command.getCmdName().equals(CommandConstants.CMD_NAME_STOP_GUEST_SCIENCE)) {
            boolean running;
            String data_msg;
            if (command.getCmdName().equals(CommandConstants.CMD_NAME_START_GUEST_SCIENCE)) {
                running = true;
                data_msg = "{\"Summary\": \"Started\"}";
            } else {
                running = false;
                data_msg = "{\"Summary\": \"Stopped\"}";
            }

            ChannelBuffer buff = data.getData();
            buff.setBytes(0, data_msg.getBytes());
            buff.setIndex(0, data_msg.length());
            data.setData(buff);

            boolean[] runningApks = state_.getRunningApks();
            if (command.getArgs().get(0).getS().equals(apk_name1_)) { // air_sampler
                runningApks[0] = running;
                data.setApkName(apk_name1_);
            } else if (command.getArgs().get(0).getS().equals(apk_name2_)) { // rfid_reader
                runningApks[1] = running;
                data.setApkName(apk_name2_);
            } else {
                msg = "Apk " + command.getArgs().get(0).getS() + " not recognized.";
                sendAck(command.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
                return;
            }

            state_.setRunningApks(runningApks);

            hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
            state_.setHeader(hdr);

            state_publisher_.publish(state_);
            data_publisher_.publish(data);
            sendAck(command.getCmdId());
        } else {
            msg = "Command " + command.getCmdName() + " is not a guest science command.";
            sendAck(command.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
        }

        try {
            listener.onMessage(out_msg);
        } catch (Exception ex) {
            Log.e(TAG, "OnMessageListener threw an exception", ex);
        }
    }

    public void changeConfig() {
        Header hdr = config_.getHeader();
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
        config_.setHeader(hdr);
        config_.setSerial((config_.getSerial() + 1));

        // Add two simulated apks
        GuestScienceApk apk = message_factory_.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(apk_name1_);
        apk.setShortName("air_sampler");
        apk.setPrimary(false);

        List<GuestScienceCommand> cmds = apk.getCommands();

        GuestScienceCommand cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Take Readings");
        cmd.setCommand("{name=Take, num=5, time_between=10}");
        cmds.add(cmd);

        apk.setCommands(cmds);

        List<GuestScienceApk> apks = config_.getApks();
        apks.clear();
        apks.add(apk);

        apk = message_factory_.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(apk_name2_);
        apk.setShortName("rfid_reader");
        apk.setPrimary(true);

        cmds = apk.getCommands();

        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Send Inventory to Ground");
        cmd.setCommand("{name=Send}");
        cmds.add(cmd);

        apk.setCommands(cmds);
        apks.add(apk);

        apk = message_factory_.newFromType(GuestScienceApk._TYPE);
        apk.setApkName("gov.nasa.arc.irg.astrobee.test_apk");
        apk.setShortName("test_apk");
        apk.setPrimary(false);

        apks.add(apk);

        config_.setApks(apks);
        config_publisher_.publish(config_);

        // Initialize the state
        boolean[] runningApks = new boolean[config_.getApks().size()];
        Arrays.fill(runningApks, false);

        hdr = state_.getHeader();
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());

        state_.setHeader(hdr);
        state_.setSerial(config_.getSerial());
        state_.setRunningApks(runningApks);
        state_publisher_.publish(state_);
    }

    interface OnMessageListener {
        void onMessage(final String msg);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("guest_science_manager_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Needs three publishers, one to ack guest science commands, one for
        // the guest science state, and one for guest science data
        // TODO(Katie) Need to figure out how to get the robot name, possibly make it a ros param

        ack_publisher_ = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_ACK, AckStamped._TYPE);

        config_publisher_ = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_CONFIG,
                GuestScienceConfig._TYPE);

        data_publisher_ = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_DATA, GuestScienceData._TYPE);

        state_publisher_ = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_STATE,
                GuestScienceState._TYPE);

        // State and config file topics are latched
        config_publisher_.setLatchMode(true);
        state_publisher_.setLatchMode(true);

        command_subscriber_ = connectedNode.newSubscriber(
                Constants.TOPIC_MANAGEMENT_EXEC_COMMAND, CommandStamped._TYPE);
        command_subscriber_.addMessageListener(this);

        node_config_ = NodeConfiguration.newPrivate();
        message_factory_ = node_config_.getTopicMessageFactory();
        config_ = config_publisher_.newMessage();
        state_ = state_publisher_.newMessage();

        apk_name1_ = "gov.nasa.arc.irg.astrobee.air_sampler";
        apk_name2_ = "gov.nasa.arc.irg.astrobee.rfid_reader";

        findApkInfo();

        rf_data_ = message_factory_.newFromType(GuestScienceData._TYPE);
        rf_data_.setApkName(apk_name2_);
        rf_data_.setDataType(GuestScienceData.JSON);
        rf_data_.setTopic("Astrobee.MessageType");
        String data = "{\"Summary\": \"SciCamera, HazCamera, NavCamera, DockCamera, PerchCamera, Laser, FrontFlashlight, BackFlashlight\"}";
        ChannelBuffer buff = rf_data_.getData();
        buff.setBytes(0, data.getBytes());
        buff.setIndex(0, data.length());
        rf_data_.setData(buff);

        internal_ = false;
    }

    public void findApkInfo() {
        // TODO(Katie) Simulated apks and commands for GDS testing, replace with actual code
        // For actual code, look into intents
        // TODO(Katie) This needs to also be called from a refresh service that
        // TODO(Katie) is used when APKs are installed and removed, need see
        // TODO(Katie) which apks are running and preserve this information
        Header hdr = config_.getHeader();
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
        config_.setHeader(hdr);
        // Serial number needs to match in the config and state messages so
        // the ground data system knows which state corresponds to the config
        // messages
        config_.setSerial(1);

        // Add two simulated apks
        GuestScienceApk apk = message_factory_.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(apk_name1_);
        apk.setShortName("air_sampler");
        apk.setPrimary(false);

        List<GuestScienceCommand> cmds = apk.getCommands();

        GuestScienceCommand cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Turn On Reader");
        cmd.setCommand("{name=On}");
        cmds.add(cmd);

        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Turn Off Reader");
        cmd.setCommand("{name=Off}");
        cmds.add(cmd);

        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Take Readings");
        cmd.setCommand("{name=Take, num=5, time_between=10}");
        cmds.add(cmd);

        apk.setCommands(cmds);

        List<GuestScienceApk> apks = config_.getApks();
        apks.add(apk);

        apk = message_factory_.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(apk_name2_);
        apk.setShortName("rfid_reader");
        apk.setPrimary(true);

        cmds = apk.getCommands();

        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Send Inventory to Ground");
        cmd.setCommand("{name=Send}");
        cmds.add(cmd);

        cmd = message_factory_.newFromType(GuestScienceCommand._TYPE);
        cmd.setName("Remove Item");
        cmd.setCommand("{name=Remove, item=SciCamera}");
        cmds.add(cmd);

        apk.setCommands(cmds);
        apks.add(apk);

        config_.setApks(apks);
        config_publisher_.publish(config_);

        // Initialize the state
        boolean[] runningApks = new boolean[config_.getApks().size()];
        Arrays.fill(runningApks, false);

        hdr = state_.getHeader();
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());

        state_.setHeader(hdr);
        state_.setSerial(config_.getSerial());
        state_.setRunningApks(runningApks);
        state_publisher_.publish(state_);
    }

    public void sendAck(String cmd_id) {
        sendAck(cmd_id, AckCompletedStatus.OK, "", AckStatus.COMPLETED);
    }

    public void sendAck(String cmd_id, byte completed_status, String message) {
        sendAck(cmd_id, completed_status, message, AckStatus.COMPLETED);
    }

    public void sendAck(String cmd_id, byte completed_status, String message,
                        byte status) {
        if (ack_publisher_ == null) {
            return;
        }

        AckStamped ack = ack_publisher_.newMessage();
        Header hdr = message_factory_.newFromType(Header._TYPE);
        hdr.setStamp(node_config_.getTimeProvider().getCurrentTime());
        ack.setHeader(hdr);

        ack.setCmdId(cmd_id);

        AckCompletedStatus ack_cs = message_factory_.newFromType(AckCompletedStatus._TYPE);
        ack_cs.setStatus(completed_status);
        ack.setCompletedStatus(ack_cs);

        ack.setMessage(message);

        AckStatus ack_status = message_factory_.newFromType(AckStatus._TYPE);
        ack_status.setStatus(status);
        ack.setStatus(ack_status);

        ack.setInternal(internal_);

        ack_publisher_.publish(ack);
    }

    public void setListener(OnMessageListener listener) {
        listener_ = listener;
    }
}
