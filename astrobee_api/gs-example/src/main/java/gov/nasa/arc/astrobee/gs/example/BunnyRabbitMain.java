package gov.nasa.arc.astrobee.gs.example;

import gov.nasa.arc.astrobee.ros.guestscience.JavaGuestScienceManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;


public class BunnyRabbitMain {
    private static final Log logger = LogFactory.getLog(BunnyRabbitMain.class);

    public static void main(String[] args) throws InterruptedException {

        JavaGuestScienceManager manager = new JavaGuestScienceManager();

        String xmlFilePath = System.getProperty("user.dir") + File.separator + "/gs-example/src/main/resources/bunny_commands.xml";
        BunnyRabbitApplication mrNibbles = new BunnyRabbitApplication(xmlFilePath);

        Thread.sleep(2000);
        manager.acceptApplication(mrNibbles);
    }


}
