
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

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;

/**
 * The current pose, velocity and acceleration of Astrobee, along with how
 * confident we are that we know this information.
 */
public interface Kinematics {

    /**
     * The confidence we have in the current state of Astrobee.
     */
    enum Confidence {
        /**
         * We are tracking features well and have a good idea of where we are.
         */
        GOOD,

        /**
         * We are not tracking as many features as we should, don't use the
         * current pose for missile tracking or open-heart surgery.
         */
        POOR,

        /**
         * We have no idea where we are. Don't listen to anything in this pose
         * message. We are *probably* within the ISS, but at this point no
         * guarantees can be made.
         */
        LOST
    }

    /**
     * Get the confidence Astrobee has in this Kinematics.
     *
     * @return {@link Confidence}
     */
    Confidence getConfidence();

    /**
     * Get the (x, y, z) position of Astrobee.
     *
     * @return A {@link Point}.
     */
    Point getPosition();

    /**
     * Get the orientation (as a {@link Quaternion}) of Astrobee.
     *
     * @return A {@link Quaternion}
     */
    Quaternion getOrientation();

    /**
     * Get the linear velocity of Astrobee along each axis (x, y and z)
     *
     * @return A {@link Vec3d} representing the velocity.
     */
    Vec3d getLinearVelocity();

    /**
     * Get the angular velocity (omega) of Astrobee along each axis.
     *
     * @return A {@link Vec3d} representing angular velocities.
     */
    Vec3d getAngularVelocity();

    /**
     * Get the linear Acceleration of Astrobee along each axis.
     *
     * @return A {@link Vec3d} representing acceleration.
     */
    Vec3d getLinearAcceleration();

}
