

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

package gov.nasa.arc.irg.astrobee.picoflexx;

final class Constants {
    private static final String PKG = Constants.class.getPackage().getName();

    public static final String EXTRA_MASTER_URI = "ROS_MASTER_URI";
    public static final String EXTRA_HOSTNAME = "ROS_HOSTNAME";

    public static final String EXTRA_ACTION = "PICOFLEXX_ACTION";
    public static final String ACTION_START = "capture_start";
    public static final String ACTION_STOP = "capture_stop";
    public static final String ACTION_DIE = "die_die_die";
}
