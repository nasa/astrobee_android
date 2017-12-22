
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

package gov.nasa.arc.astrobee;

import gov.nasa.arc.astrobee.internal.BaseRobot;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

public interface Robot extends BaseRobot {

    /**
     * Move the robot to the given position and orientation.
     *
     * Note: The position is absolute within the ISS, not relative to the robot.
     *
     * @param xyz The point within the ISS it should go to.
     * @param rot The orientation of Astrobee after the move completes.
     * @return {@link PendingResult} of the command.
     */
    PendingResult simpleMove6DOF(Point xyz, Quaternion rot);

    /**
     * Get the last received kinematic state of the robot. (That is: pose,
     * velocity and acceleration.)
     *
     * Note: Before we receive our first location update, this method will
     * return a "Lost" state that is not really useful.
     *
     * @return The {@link Kinematics}
     */
    Kinematics getCurrentKinematics();

}
