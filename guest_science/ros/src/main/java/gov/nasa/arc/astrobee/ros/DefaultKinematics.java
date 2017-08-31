
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

package gov.nasa.arc.astrobee.ros;

import ff_msgs.EkfState;
import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;
import gov.nasa.arc.astrobee.types.Vec3d;

public class DefaultKinematics implements Kinematics {
    final private Kinematics.Confidence m_confidence;
    final private Point m_position;
    final private Quaternion m_orientation;
    final private Vec3d m_linearVelocity;
    final private Vec3d m_angularVelocity;
    final private Vec3d m_linearAcceleration;

    public DefaultKinematics(final EkfState ekf) {
        m_position = new Point(
                ekf.getPose().getPosition().getX(),
                ekf.getPose().getPosition().getY(),
                ekf.getPose().getPosition().getZ()
        );
        m_orientation = new Quaternion(
                (float) ekf.getPose().getOrientation().getX(),
                (float) ekf.getPose().getOrientation().getY(),
                (float) ekf.getPose().getOrientation().getZ(),
                (float) ekf.getPose().getOrientation().getW()
        );
        m_linearVelocity = new Vec3d(
                ekf.getVelocity().getX(),
                ekf.getVelocity().getY(),
                ekf.getVelocity().getZ()
        );
        m_angularVelocity = new Vec3d(
                ekf.getOmega().getX(),
                ekf.getOmega().getY(),
                ekf.getOmega().getZ()
        );
        m_linearAcceleration = new Vec3d(
                ekf.getAccel().getX(),
                ekf.getAccel().getY(),
                ekf.getAccel().getZ()
        );

        switch(ekf.getConfidence()) {
            case EkfState.CONFIDENCE_GOOD:
                m_confidence = Confidence.GOOD;
                break;
            case EkfState.CONFIDENCE_POOR:
                m_confidence = Confidence.POOR;
                break;
            case EkfState.CONFIDENCE_LOST:
                m_confidence = Confidence.LOST;
                break;
            default:
                throw new IllegalArgumentException("Invalid condifence byte?");
        }
    }

    public DefaultKinematics() {
        m_position = new Point();
        m_orientation = new Quaternion();
        m_linearVelocity = new Vec3d();
        m_angularVelocity = new Vec3d();
        m_linearAcceleration = new Vec3d();
        m_confidence = Confidence.LOST;
    }

    @Override
    public Point getPosition() {
        return m_position;
    }

    @Override
    public Quaternion getOrientation() {
        return m_orientation;
    }

    @Override
    public Confidence getConfidence() {
        return m_confidence;
    }

    @Override
    public Vec3d getLinearVelocity() {
        return m_linearVelocity;
    }

    @Override
    public Vec3d getAngularVelocity() {
        return m_angularVelocity;
    }

    @Override
    public Vec3d getLinearAcceleration() {
        return m_linearAcceleration;
    }
}
