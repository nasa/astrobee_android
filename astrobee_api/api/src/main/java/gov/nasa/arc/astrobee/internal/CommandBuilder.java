
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

package gov.nasa.arc.astrobee.internal;

import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Vec3d;

public interface CommandBuilder {
    CommandBuilder setName(String name);

    CommandBuilder setSubsystem(String subsystem);

    CommandBuilder addArgument(String name, float value);

    CommandBuilder addArgument(String name, double value);

    CommandBuilder addArgument(String name, int value);

    CommandBuilder addArgument(String name, long value);

    CommandBuilder addArgument(String name, boolean value);

    CommandBuilder addArgument(String name, String value);

    CommandBuilder addArgument(String name, Vec3d value);

    CommandBuilder addArgument(String name, Mat33f value);

    /**
     * Add an enumerated argument to a command. The {@link Enum#toString()} should
     * return the appropriate value for the argument.
     *
     * @param name The name of the argument
     * @param value The {@link Enum} value of the argument
     * @param <E> The {@link Enum} that is the type of the argument
     * @return The current {@link CommandBuilder}, "this"
     */
    <E extends Enum<E>>
    CommandBuilder addArgument(String name, E value);

    Publishable build();
}
