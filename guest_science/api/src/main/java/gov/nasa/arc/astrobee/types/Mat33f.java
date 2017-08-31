
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

public class Mat33f {
    protected float m_vec[];

    public Mat33f(final float vec[]) {
       if (vec == null)
           throw new NullPointerException("vec may not be null");
       if (vec.length != 9)
           throw new IllegalArgumentException("vec must be length 9");
       m_vec = vec.clone();
    }

    public float[] toArray() {
        return m_vec.clone();
    }
}
