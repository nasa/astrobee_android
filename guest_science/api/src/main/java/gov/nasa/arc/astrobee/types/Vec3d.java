
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

import java.text.DecimalFormat;

public class Vec3d {
    protected final double m_vec[];

    public Vec3d() {
        this(0.0, 0.0, 0.0);
    }

    public Vec3d(double x, double y, double z) {
        m_vec = new double[]{ x, y, z };
    }

    public Vec3d(final double vec[]) {
        if (vec == null)
            throw new NullPointerException("vec must not be null");
        if (vec.length != 3)
            throw new IllegalArgumentException("array must be of size 3");
        m_vec = vec.clone();
    }

    public double[] toArray() {
        return m_vec.clone();
    }

    private static final DecimalFormat s_decimalFormatter = new DecimalFormat("#.###");

    @Override
    public String toString() {
        final String clzName = this.getClass().getName();
        return clzName + "[" + s_decimalFormatter.format(m_vec[0]) +
                        ", " + s_decimalFormatter.format(m_vec[1]) +
                        ", " + s_decimalFormatter.format(m_vec[2]) + "]";
    }
}
