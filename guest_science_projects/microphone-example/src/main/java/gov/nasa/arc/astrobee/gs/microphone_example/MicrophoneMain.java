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

package gov.nasa.arc.astrobee.ros.microphone_example;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.ros.guestscience.JavaGuestScienceManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.io.File;

public class MicrophoneMain {

    private static final Log logger = LogFactory.getLog(MicrophoneMain.class);

    public static void main(String[] args) throws InterruptedException {

        // Because log4j doesn't do what is needed.
        Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler());

        // In this mode we will fake the bot to get quickly a plausible result.
        // This is useful for debugging as the simulation is otherwise very slow.
        boolean fastMode = false;

        // Avoid the Guest Science manager when in debugging mode.
        boolean skipGSManager = false;

        // Assume we are running this on the Gateway instead of the JPM module.
        boolean useGateway = false;

        if (fastMode || skipGSManager) {
            MicrophoneImplementation impl
            = MicrophoneImplementation.getInstance(fastMode, skipGSManager, useGateway);
        }
        else{
            JavaGuestScienceManager manager = new JavaGuestScienceManager();

            String xmlFilePath = System.getProperty("user.dir") +
            File.separator + "/src/main/resources/microphone_commands.xml";
            MicrophoneApplication microphoneApp = new MicrophoneApplication(xmlFilePath);

            Thread.sleep(2000);
            manager.acceptApplication(microphoneApp);
        }
    }
}
