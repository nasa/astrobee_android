
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

package gov.nasa.arc.astrobee.ros.example;

import gov.nasa.arc.astrobee.*;
import gov.nasa.arc.astrobee.ros.RobotConfiguration;
import gov.nasa.arc.astrobee.ros.DefaultRobotFactory;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.TimeUnit;

public class ExampleMain {
    private static final Log logger = LogFactory.getLog(ExampleMain.class);

    public static void main(String[] args) throws InterruptedException, AstrobeeException {
        // Because log4j doesn't do the needful
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler());

        RobotFactory factory = new DefaultRobotFactory();
        System.out.println("I am " + factory.getLocalName());
        Robot self;

        try {
            self = factory.getRobot();

            logger.info("Waiting for robot to acquire position");
            Kinematics k;
            while (true) {
                k = self.getCurrentKinematics();
                if (k.getConfidence() == Kinematics.Confidence.GOOD)
                    break;
                Thread.sleep(250);
            }

            logger.info("Moving the bee");
            Point currPosition = k.getPosition();
            Quaternion currOrient = k.getOrientation();

            Point endPoint = new Point(currPosition.getX() + 0.5, currPosition.getY(), currPosition.getZ());

            PendingResult pending = self.simpleMove6DOF(endPoint, currOrient);
            while (!pending.isFinished()) {
                k = self.getCurrentKinematics();
                logger.info("Current Position: " + k.getPosition().toString());
                pending.getResult(1000, TimeUnit.MILLISECONDS);
            }

            Result result = pending.getResult();
            logger.info("Command status: " + result.getStatus().toString());
            if (!result.hasSucceeded()) {
                logger.info("Command message: " + result.getMessage());
            }

            logger.info("Done");

            factory.shutdown();
        } catch (Exception e) {
            logger.info("Error, woops", e);
        }
    }
}
