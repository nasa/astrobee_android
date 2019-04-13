package gov.nasa.arc.astrobee.ros.guestscience;

import ff_msgs.*;
import gov.nasa.arc.astrobee.AstrobeeRuntimeException;
import gov.nasa.arc.astrobee.ros.NodeExecutorHolder;
import gov.nasa.arc.astrobee.ros.internal.util.CmdInfo;
import gov.nasa.arc.astrobee.ros.internal.util.CmdType;
import gov.nasa.arc.astrobee.ros.internal.util.Constants;
import gov.nasa.arc.astrobee.ros.internal.util.MessageType;
import gov.nasa.arc.astrobee.ros.internal.util.Stringer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import std_msgs.Header;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

//import org.w3c.dom.Node;


public class GuestScienceNodeMain extends AbstractNodeMain implements MessageListener<CommandStamped> {
    private final Log logger = LogFactory.getLog(GuestScienceNodeMain.class);

    private ConnectedNode m_node = null;
    private Publisher<AckStamped> m_ackStampedPublisher = null;
    private Publisher<GuestScienceConfig> m_gsConfigPublisher = null;
    private Publisher<GuestScienceData> m_gsDataPublisher = null;
    private Publisher<GuestScienceState> m_gsStatePublisher = null;

    private Subscriber<CommandStamped> mCommandSubscriber;

    private StartGuestScienceService m_currentApplication;

    private boolean m_started = false;
    private boolean m_apkIsRunning = false;
    private String m_apkName; // for convenience
    private long SERIAL_NUMBER = 1;

    private NodeConfiguration mNodeConfig;
    private MessageFactory mMessageFactory;

    private CmdInfo mCmdInfo;

    @Override
    public synchronized void onStart(final ConnectedNode connectedNode) {
        m_node = connectedNode;

        mNodeConfig = NodeConfiguration.newPrivate();
        mMessageFactory = mNodeConfig.getTopicMessageFactory();

        m_gsConfigPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_CONFIG, GuestScienceConfig._TYPE);
        m_ackStampedPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_ACK, AckStamped._TYPE);
        m_gsDataPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_DATA, GuestScienceData._TYPE);
        m_gsStatePublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_STATE,
                GuestScienceState._TYPE);

        // State and config file topics are latched
        m_gsConfigPublisher.setLatchMode(true);
        m_gsStatePublisher.setLatchMode(true);

        mCommandSubscriber = connectedNode.newSubscriber(
                Constants.TOPIC_MANAGEMENT_EXEC_COMMAND, CommandStamped._TYPE);
        mCommandSubscriber.addMessageListener(this);

        mCmdInfo = new CmdInfo();

        m_started = true;
    }

    @Override
    public void onNewMessage(CommandStamped cmd) {
        // Command syntax checked in executive
        if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_CUSTOM_GUEST_SCIENCE)) {
            handleGuestScienceCustomCommand(cmd);

        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_STOP_GUEST_SCIENCE)) {
            handleGuestScienceStopCommand(cmd);

        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_START_GUEST_SCIENCE)) {
            handleGuestScienceStartCommand(cmd);

        } else {
            String msg = "Command " + cmd.getCmdName() + " is not a guest science command.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
        }
    }

    protected void shutdown()  {
    	this.logger.debug("Attempting to shutdown node");
    	NodeExecutorHolder.getExecutor().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                NodeExecutorHolder.getExecutor().shutdownNodeMain(GuestScienceNodeMain.this);
            }
        });
    }

    protected void handleGuestScienceStartCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String msg;
        if(m_apkIsRunning) {
            msg =  "Apk " + m_apkName + " is already running.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            return;
        }

        m_apkIsRunning = true;


        mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), m_apkName, CmdType.START);

        ackGuestScienceStart(true, m_apkName, "");
        m_currentApplication.onGuestScienceStart();
    }

    protected void handleGuestScienceStopCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String msg;
        if(!m_apkIsRunning) {
            msg =  "Apk " + m_apkName + " is already stopped.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            return;
        }

        m_apkIsRunning = false;

        mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), m_apkName, CmdType.STOP);

        ackGuestScienceStop(true, m_apkName, "");
        m_currentApplication.onGuestScienceStop();
    }

    protected void handleGuestScienceCustomCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String command = cmd.getArgs().get(1).getS();

        sendAck(cmd.getCmdId());
        m_currentApplication.onGuestScienceCustomCmd(command);
    }

    @Override
    public synchronized void onShutdown(org.ros.node.Node node) {
        m_node = null;
        m_gsConfigPublisher = null;
        m_ackStampedPublisher = null;
        m_gsDataPublisher = null;
        m_gsStatePublisher = null;
    }

    @Override
    public void onShutdownComplete(Node node) {
    	m_started = false;
    }

    synchronized MessageFactory getTopicMessageFactory() {
        if (m_node == null)
            throw new AstrobeeRuntimeException("Node is not ready or died");
        return m_node.getTopicMessageFactory();
    }

    public void sendAck(String cmdId) {
        sendAck(cmdId, AckCompletedStatus.OK, "", AckStatus.COMPLETED);
    }

    public void sendAck(String cmdId, byte completedStatus, String message) {
        if(completedStatus == AckCompletedStatus.EXEC_FAILED || completedStatus == AckCompletedStatus.BAD_SYNTAX) {
            logger.error(message);
        }
        sendAck(cmdId, completedStatus, message, AckStatus.COMPLETED);
    }

    // does this need to be synchronized???
    synchronized public void sendAck(String cmdId, byte completedStatus, String message,
                                     byte status) {
        if (m_ackStampedPublisher == null)
            throw new AstrobeeRuntimeException("Node not ready or dead");

        AckStamped ack = m_ackStampedPublisher.newMessage();
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(m_node.getCurrentTime());
        ack.setHeader(hdr);
        ack.setCmdId(cmdId);
        ack.setMessage(message);

        AckCompletedStatus ackCS = mMessageFactory.newFromType(AckCompletedStatus._TYPE);
        ackCS.setStatus(completedStatus);
        ack.setCompletedStatus(ackCS);

        AckStatus ackStatus = mMessageFactory.newFromType(AckStatus._TYPE);
        ackStatus.setStatus(status);
        ack.setStatus(ackStatus);

        m_ackStampedPublisher.publish(ack);
    }

    public void ackGuestScienceStart(boolean started, String apkName, String errMsg) {
        if (started) {
            m_apkIsRunning = true;
            sendGuestScienceState();
            sendAck(mCmdInfo.mId);
        } else {
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
        }
        mCmdInfo.resetCmd();
    }

    public void ackGuestScienceStop(boolean stopped, String apkName, String errMsg) {
        if (stopped) {
            m_apkIsRunning = false;
            sendGuestScienceState();
            sendAck(mCmdInfo.mId);
        } else {
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
        }
        mCmdInfo.resetCmd();
    }

    public void sendGuestScienceState() {
        GuestScienceState mState = m_gsStatePublisher.newMessage();
        boolean[] runningApks = {m_apkIsRunning};
        mState.setRunningApks(runningApks);
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        mState.setHeader(hdr);
        mState.setSerial(SERIAL_NUMBER);
        m_gsStatePublisher.publish(mState);
    }

    synchronized void publishGuestScienceConfig(StartGuestScienceService app) {
        if (m_gsConfigPublisher == null)
            throw new AstrobeeRuntimeException("Node not ready or dead");

        m_currentApplication = app;
        m_apkName = app.getFullName();
        GuestScienceApk apk = mMessageFactory.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(app.getFullName());
        apk.setShortName(app.getShortName());
        apk.setPrimary(app.isPrimary());

        List<GuestScienceCommand> cmds = new ArrayList<>();

        if (app.getCommands() != null) {
            for (Command gsCmd : app.getCommands()) {
                GuestScienceCommand cmd = mMessageFactory.newFromType(GuestScienceCommand._TYPE);
                cmd.setName(gsCmd.getName());
                cmd.setCommand(gsCmd.getSyntax());
                cmds.add(cmd);
            }
        }
        apk.setCommands(cmds);

        GuestScienceConfig mConfig = m_gsConfigPublisher.newMessage();
        try {
            mConfig.getHeader().setStamp(m_node.getCurrentTime());
        } catch (NullPointerException e) {
            mConfig.getHeader().setStamp(new org.ros.message.Time());
        }
        mConfig.setSerial(SERIAL_NUMBER);
        List<GuestScienceApk> apks = new ArrayList<>();
        apks.add(apk);
        mConfig.setApks(apks);

        logger.debug("Publishing " + Stringer.toString(mConfig));
        m_gsConfigPublisher.publish(mConfig);

    }

    /* Call this to send data from apk to ground */
    public void sendGuestScienceData(String apkFullName, String topic, byte[] data, MessageType dataType) {
        if (topic.length() > 32) {
            logger.error("The topic string in the guest science message is too " +
                    "big to send to the ground so the message will not be sent. Length must " +
                    " be no more than 32 characters not " + topic.length() + ".");
            return;
        }

        if (data.length > 2048) {
            logger.error("The data in the guest science message is too big to send " +
                    "to the ground so the message will not be sent. Length of data must be no" +
                    " more than 2048 bytes not " + data.length + ".");
            return;
        }

        GuestScienceData dataMsg = mMessageFactory.newFromType(GuestScienceData._TYPE);
        Header hdr = mMessageFactory.newFromType(Header._TYPE);

        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        dataMsg.setHeader(hdr);

        dataMsg.setApkName(apkFullName);

        if (dataType == MessageType.STRING) {
            dataMsg.setDataType(GuestScienceData.STRING);
        } else if (dataType == MessageType.JSON) {
            dataMsg.setDataType(GuestScienceData.JSON);
        } else if (dataType == MessageType.BINARY) {
            dataMsg.setDataType(GuestScienceData.BINARY);
        } else {
            logger.error("Message type in guest science message is unknown so the message " +
                    "will not be sent to the ground.");
            return;
        }

        dataMsg.setTopic(topic);

        ChannelBuffer dataBuff = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, data);
        dataMsg.setData(dataBuff);
        m_gsDataPublisher.publish(dataMsg);
    }

    protected boolean validateApkName(CommandStamped cmd) {
        String incomingApkName = cmd.getArgs().get(0).getS();
        if(m_apkName == null || !m_apkName.equals(incomingApkName)) {
            String msg = "Unknown apk " + incomingApkName + ".";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            logger.error(msg);
            return false;
        }
        return true;
    }

    public boolean isStarted() {
        return m_started;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("gs_manager_stub");
    }
}
