package gov.nasa.arc.astrobee.gs.example;

import gov.nasa.arc.astrobee.ros.guestscience.*;
import gov.nasa.arc.astrobee.ros.internal.util.MessageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BunnyRabbitApplication extends StartGuestScienceService {
    private static final Log logger = LogFactory.getLog(BunnyRabbitApplication.class);

    public BunnyRabbitApplication(String xmlFilePath) {
        super(xmlFilePath);
    }

    @Override
    public void onGuestScienceCustomCmd(String command) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"Summary\": \"");

        if (command.contains("action")) {
            String[] tokens = command.split("\"");
            sb.append(tokens[3] + " away!\"}");
        } else {
            sb.append("Unknown command\"}");
        }
        sendData(MessageType.JSON,
                "Rabbit Life",
                sb.toString());
    }

    @Override
    public void onGuestScienceStart() {
        sendStarted("Rabbit Info");
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("Rabbit Info");
    }
}
