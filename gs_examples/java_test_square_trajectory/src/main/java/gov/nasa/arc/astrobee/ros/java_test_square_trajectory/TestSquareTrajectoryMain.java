package gov.nasa.arc.astrobee.ros.java_test_square_trajectory;

import java.io.File;
import gov.nasa.arc.astrobee.ros.guestscience.JavaGuestScienceManager;

public class TestSquareTrajectoryMain {

    public static void main(String[] args) throws InterruptedException {
    	JavaGuestScienceManager manager = new JavaGuestScienceManager();

        String xmlFilePath = System.getProperty("user.dir") + File.separator + "/src/main/resources/commands.xml";
        TestSquareTrajectoryApplication app = new TestSquareTrajectoryApplication(xmlFilePath);

        Thread.sleep(2000);
        manager.acceptApplication(app);
    }
}
