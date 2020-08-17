
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

package gov.nasa.arc.astrobee.signal_intention_state;

public class VideoStateConfig {

    public static final String TYPE_RUNNER = "app_runner";
    public static final String TYPE_IDLE = "app_idle";
    public static final String TYPE_LOOP = "loop";
    public static final String TYPE_SEQUENTIAL = "sequential";

    private String type;
    private String rosState;
    private String localVideoName;
    private String nextDefaultState;

    public VideoStateConfig() {
        type = null;
        rosState = null;
        localVideoName = null;
        nextDefaultState = null;
    }

    public VideoStateConfig(String type, String rosState, String localVideoName,
                            String nextDefaultState) {
        this.type = type;
        this.rosState = rosState;
        this.localVideoName = localVideoName;
        this.nextDefaultState = nextDefaultState;
    }

    public boolean isValid() {
        // Type and rosstate are compulsory
        boolean valid =  type != null && rosState != null;

        // Loop cannot have a next default state and must have a video
        if (valid && type.equals(TYPE_LOOP) && (nextDefaultState != null || localVideoName == null))
            valid = false;

        // Sequential must have all four states
        if (valid && type.equals(TYPE_SEQUENTIAL) && (nextDefaultState == null || localVideoName == null))
            valid = false;

        return valid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRosState() {
        return rosState;
    }

    public String getLocalVideoName() {
        return localVideoName;
    }

    public String getNextDefaultState() {
        return nextDefaultState;
    }

    public void setRosState(String rosState) {
        this.rosState = rosState;
    }

    public void setLocalVideoName(String localVideoName) {
        this.localVideoName = localVideoName;
    }

    public void setNextDefaultState(String nextDefaultState) {
        this.nextDefaultState = nextDefaultState;
    }
}
