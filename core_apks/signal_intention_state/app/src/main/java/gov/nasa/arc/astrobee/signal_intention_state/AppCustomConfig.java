
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

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

public class AppCustomConfig {

    private ArrayList<VideoStateConfig> videoMappingConfig;
    private VideoStateConfig defaultState;
    private Context context;

    public AppCustomConfig(Context context) {
        this.context = context;
    }

    public boolean loadConfig() {
        try {
            JSONObject jConfig = parseJSONConfigFile();

            // Parse possible states
            ArrayList<VideoStateConfig> arrayConfig = mapJSON(jConfig);

            if (arrayConfig == null || arrayConfig.isEmpty()) {
                this.videoMappingConfig = null;
                return false;
            }
            this.videoMappingConfig = arrayConfig;

            // Select default state
            this.defaultState = getStateConfig(jConfig.getString("default_state"));
            return true;

        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ArrayList<VideoStateConfig> mapJSON(JSONObject jsonObject) throws JSONException {
        JSONArray signalStates = jsonObject.getJSONArray("signal_states");
        ArrayList<VideoStateConfig> videoConfig = new ArrayList<>();

        for (int i = 0; i < signalStates.length(); i++) {
            JSONObject jConfig = signalStates.getJSONObject(i);

            String type = jConfig.has("type") ? jConfig.getString("type") : null;
            String rosState = jConfig.has("ros_state_name")
                    ? jConfig.getString("ros_state_name") : null;
            String localVideoName = jConfig.has("local_video_name")
                    ? jConfig.getString("local_video_name") : null;
            String nextDefaultState = jConfig.has("next_default_state")
                    ? jConfig.getString("next_default_state") : null;

            VideoStateConfig videoStateConfig = new VideoStateConfig(type, rosState, localVideoName,
                    nextDefaultState);

            videoConfig.add(videoStateConfig);
        }

        return videoConfig;

    }

    private JSONObject parseJSONConfigFile() throws IOException, JSONException {
        InputStream inputStream;
        File externalConfigFile = new File(context.getExternalFilesDir(null), "config.json");

        if (externalConfigFile.exists()) {
            inputStream = new FileInputStream(externalConfigFile);
        } else {
            inputStream = context.getAssets().open("config.json");
        }

        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader buffReader = new BufferedReader(inputReader);
        String line;
        StringBuilder text = new StringBuilder();

        while ((line = buffReader.readLine()) != null) {
            text.append(line);
        }

        return new JSONObject(text.toString());
    }

    public VideoStateConfig getDefaultState() {
        return defaultState;
    }

    public VideoStateConfig getStateConfig(String state) {
        VideoStateConfig stateConfig = null;

        for (VideoStateConfig config : videoMappingConfig) {
            if (config.getRosState().equals(state)) {
                stateConfig = config;
                break;
            }
        }

        return stateConfig;
    }

    public VideoStateConfig getRunnerState() {
        VideoStateConfig stateConfig = null;

        for (VideoStateConfig config : videoMappingConfig) {
            if (config.getType().equals(VideoStateConfig.TYPE_RUNNER)) {
                stateConfig = config;
                break;
            }
        }

        return stateConfig;
    }
}
