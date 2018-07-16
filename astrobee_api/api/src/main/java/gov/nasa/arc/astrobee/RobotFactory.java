
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface RobotFactory {

    /**
     * Get "this" robot, as opposed to a remote robot. Much higher
     * chance of success.
     *
     * @return This {@link Robot}.
     * @throws AstrobeeException Thrown when it can't contact the
     *         ROS master or something else fatal
     * @throws InterruptedException Because Java....
     */
    Robot getRobot() throws AstrobeeException, InterruptedException;

    /**
     * Get a robot by name. If {@code name} is {@code null} or the empty string,
     * then this acts like
     * {@link RobotFactory#getRobot() getRobot()} and gets "this" robot.
     *
     * Getting "remote" robots has a much higher chance of failure. You may
     * want to actually use {@link RobotFactory#getRobot(String, long, TimeUnit)}
     * instead of this version.
     *
     * @param name The name of the robot or {@code null} for "this" robot.
     * @return The {@link Robot} with the given name.
     * @throws AstrobeeException In case we can't contact the robot.
     * @throws InterruptedException Because Java....
     */
    Robot getRobot(String name) throws AstrobeeException, InterruptedException;

    /**
     * Get "this" robot, as opposed to a remote robot, but only wait the given
     * amount of time before giving up on love, life and everything you thought
     * mattered in your insignificant existence.
     *
     * @param timeout How long to wait
     * @param units The units of the above timeout
     * @return This {@link Robot}.
     * @throws AstrobeeException Thrown if it can't contact the ROS master or
     *         something else Astrobee-specific.
     * @throws InterruptedException Thrown because Java.
     * @throws TimeoutException If we can't establish contact within the given
     *         timeout given.
     */
    Robot getRobot(long timeout, TimeUnit units) throws AstrobeeException, InterruptedException, TimeoutException;

    /**
     * Get a robot by name. If {@code name} is {@code null} or the empty string,
     * then this acts like
     * {@link RobotFactory#getRobot(long, TimeUnit) getRobot(long, TimeUnit)}
     * and gets "this" robot.
     *
     * Contacting remote robots has a much higher probability of failure.
     *
     * This version will timeout if the specified robot cannot be contacted
     * within the given timeout. You should probably use this one.
     *
     * @param name
     * @param timeout
     * @param units
     * @return A {@link Robot} with the given name.
     * @throws AstrobeeException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    Robot getRobot(String name, long timeout, TimeUnit units) throws AstrobeeException, InterruptedException, TimeoutException;

    String getLocalName();

    /**
     * Shut down the internal machinery, allowing Java to exit cleanly.
     */
    void shutdown();
}
