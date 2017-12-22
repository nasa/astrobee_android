
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

package gov.nasa.arc.astrobee.types;

import gov.nasa.arc.astrobee.types.Mat33f;

import java.text.DecimalFormat;

public class Quaternion extends Mat33f {
    /**
     * Define a new quaternion representing no rotation around an undefined axis:
     * (0, 0, 0, 1)
     */
    public Quaternion() {
        this(0, 0, 0, 1);
    }

    /**
     * Define a new quaternion with the given parameters
     *
     * @param x The x component of the quaternion.
     * @param y The y component of the quaternion.
     * @param z The z component of the quaternion.
     * @param w The w component of the quaternion.
     */
    public Quaternion(float x, float y, float z, float w) {
        super(new float[]{ x, y, z, w, 0, 0, 0, 0, 0 });
    }

    public float getX() {
        return m_vec[0];
    }

    public float getY() {
        return m_vec[1];
    }

    public float getZ() {
        return m_vec[2];
    }

    public float getW() {
        return m_vec[3];
    }

    private static final DecimalFormat s_decimalFormatter = new DecimalFormat("#.###");

    @Override
    public String toString() {
        return "Mat33f::Quaternion{ " +
                "x=" + s_decimalFormatter.format(m_vec[0]) + "; " +
                "y=" + s_decimalFormatter.format(m_vec[1]) + "; " +
                "z=" + s_decimalFormatter.format(m_vec[2]) + "; " +
                "w=" + s_decimalFormatter.format(m_vec[3]) + "}";
    }
}
