package gov.nasa.arc.astrobee.ros.guestscience;

import ff_msgs.GuestScienceData;
import gov.nasa.arc.astrobee.ros.internal.util.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * This is what they will end up implementing
 */
public abstract class StartGuestScienceService {

    private JavaGuestScienceManager m_manager;

    private List<Command> m_commands = new ArrayList<>();

    private String m_fullName;
    private String m_shortName;
    private boolean m_primary;

    public StartGuestScienceService(String xmlFilePath) {
        ApkInfo apkInfo = ApkInfoXmlParser.parseFile(xmlFilePath);
        m_fullName = apkInfo.getFullName();
        m_shortName = apkInfo.getShortName();
        m_primary = apkInfo.isPrimary();
        m_commands = apkInfo.getCommands();
    }

    public abstract void onGuestScienceStart();

    public abstract void onGuestScienceStop();

    public abstract void onGuestScienceCustomCmd(String command);

    public String getFullName() {
        return m_fullName;
    }

    public String getShortName() {
        return m_shortName;
    }

    public boolean isPrimary() {
        return m_primary;
    }

    public void sendData(MessageType type, String topic, String data) {
        m_manager.sendData(type, topic, data);
    }

    public void sendData(MessageType type, String topic, byte[] data) {
        m_manager.sendData(type, topic, data);
    }

    public void sendStarted(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Started\"}");
    }

    public void sendStopped(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Stopped\"}");
    }

    public void sendReceivedCustomCommand(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Received Custom Commands\"}");
    }

    public List<Command> getCommands() {
        return m_commands;
    }

    void acceptManager(JavaGuestScienceManager manager) {
        m_manager = manager;
    }
}


