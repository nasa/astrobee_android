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
import gov.nasa.arc.astrobee.Kinematics;

import gov.nasa.arc.astrobee.ros.guestscience.*;
import gov.nasa.arc.astrobee.ros.internal.util.MessageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import gov.nasa.arc.astrobee.types.PlannerType;

// This class will get commands from the user via the guest science
// interface in GDS that will tell the robot what to do.
public class MicrophoneApplication extends StartGuestScienceService {

    private static final Log logger = LogFactory.getLog(MicrophoneApplication.class);
    MicrophoneImplementation m_impl = null;

    public MicrophoneApplication(String xmlFilePath) {
        super(xmlFilePath);
    }

    @Override
    public void onGuestScienceCustomCmd(String json_str) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"Summary\": \"");

        if (json_str.contains("action")) {
            String[] tokens = json_str.split("\"");
            String command = tokens[3];

            logger.info("Command is " + command);

            if (command.equals("search") || command.equals("search_and_refine")) {
                m_impl.initialSearch();
                sb.append("finished initial search" + "\"}");
            }

            if (command.equals("refine") || command.equals("search_and_refine")) {
                boolean ans = m_impl.refinedSearch();
                if (ans)
                    sb.append("Refined search was successful." + "\"}");
                else
                    sb.append("Must do an initial search first." + "\"}");
            }

        }
        else {
            sb.append("Unknown command: " + json_str + "\"}");
        }
        
        // NOTE: This is where the GuestScienceData message is published
        sendData(MessageType.JSON, "Custom command: ", sb.toString());
    }

    @Override
    public void onGuestScienceStart() {

        // Get a unique instance of the microphone implementation
        boolean fastMode = false;
        boolean skipGSManager = false;
        boolean useGateway = false;
        m_impl = MicrophoneImplementation.getInstance(fastMode, skipGSManager, useGateway);
        sendStarted("Guest science started.");
    }

    @Override
    public void onGuestScienceStop() {
        m_impl.shutdownFactory();
        sendStopped("Guest science stopped.");
    }
}
