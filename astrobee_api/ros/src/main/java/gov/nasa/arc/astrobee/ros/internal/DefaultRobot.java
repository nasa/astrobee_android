
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

package gov.nasa.arc.astrobee.ros.internal;

import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.internal.CommandBuilder;
import gov.nasa.arc.astrobee.internal.Publishable;
import gov.nasa.arc.astrobee.internal.RobotImpl;

public class DefaultRobot extends RobotImpl {
    private final RobotNodeMain m_nodeMain;

    public DefaultRobot(final RobotNodeMain node) {
        m_nodeMain = node;
    }

    @Override
    protected CommandBuilder makeCommandBuilder() {
        return new DefaultCommandBuilder(m_nodeMain.getTopicMessageFactory());
    }

    @Override
    protected PendingResult publish(Publishable cmd) {
        return m_nodeMain.publish(
                ((CommandHolder) cmd).getCommand());
    }

    @Override
    public Kinematics getCurrentKinematics() {
        return m_nodeMain.getKinematics();
    }
}
