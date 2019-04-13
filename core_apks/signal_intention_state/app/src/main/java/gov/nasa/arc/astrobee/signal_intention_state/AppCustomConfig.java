package gov.nasa.arc.astrobee.signal_intention_state;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by rgarciar on 5/17/18.
 */

public class AppCustomConfig {

    private ArrayList<VideoStateConfig> videoMappingConfig;
    private Context context;

    public AppCustomConfig(Context context) {
        this.context = context;
    }

    public boolean loadConfig() {
        try {
            JSONObject jConfig = parseJSONConfigFile();
            ArrayList<VideoStateConfig> arrayConfig = mapJSON(jConfig);

            if (arrayConfig == null || arrayConfig.isEmpty()) {
                this.videoMappingConfig = null;
                return false;
            } else {
                this.videoMappingConfig = arrayConfig;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ArrayList<VideoStateConfig> mapJSON(JSONObject jsonObject) throws JSONException {
        JSONArray signalStates = jsonObject.getJSONArray("signal_states");
        ArrayList<VideoStateConfig> videoConfig = new ArrayList<>();

        for (int i = 0; i < signalStates.length(); i++) {
            JSONObject jConfig = signalStates.getJSONObject(i);

            String rosState = jConfig.has("ros_state_name") ? jConfig.getString("ros_state_name") : null;
            String localVideoName = jConfig.has("local_video_name") ? jConfig.getString("local_video_name") : null;
            boolean loop = jConfig.has("loop") ? jConfig.getBoolean("loop") : false;
            String nextDefaultState = jConfig.has("next_default_state") ? jConfig.getString("next_default_state") : null;
            boolean appRunner = jConfig.has("app_runner") ? jConfig.getBoolean("app_runner") : false;
            boolean appStopper = jConfig.has("app_stopper") ? jConfig.getBoolean("app_stopper") : false;

            VideoStateConfig videoStateConfig = new VideoStateConfig(rosState, localVideoName, loop, nextDefaultState, appRunner, appStopper);

            videoConfig.add(videoStateConfig);
        }

        return videoConfig;

    }

    private JSONObject parseJSONConfigFile() throws IOException, JSONException {

        InputStream inputStream = context.getAssets().open("config.json");

        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader buffReader = new BufferedReader(inputReader);
        String line;
        StringBuilder text = new StringBuilder();

        while ((line = buffReader.readLine()) != null) {
            text.append(line);
        }

        return new JSONObject(text.toString());
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
            if (config.isAppRunner()) {
                stateConfig = config;
                break;
            }
        }

        return stateConfig;
    }
}
