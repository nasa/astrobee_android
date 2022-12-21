
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

import android.app.Activity;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;
import java.io.File;

import ff_msgs.SignalState;

public class MainActivity extends Activity {

    public final static String ACTION_STOP =
            "gov.nasa.arc.astrobee.signal_intention_state.ACTION_STOP";

    private static final int UPDATE_STATE_QUEUED = 2;
    private static final int UPDATE_STATE_CHANGED = 1;
    private static final int UPDATE_STATE_ERROR = 0;
    private static final int UPDATE_STATE_IGNORED = 3;
    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };
    private boolean isFullScreen;
    private VideoView videoView;
    private AppCustomConfig config = null;
    private VideoStateConfig currentStateVideo = null;
    private int videoSavedPosition = 0;

    public static boolean isActivityRunning(Context ctx) {
        ActivityManager activityManager = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.AppTask> tasks = activityManager.getAppTasks();

        for (ActivityManager.AppTask task : tasks) {
            if (task.getTaskInfo().baseActivity == null) {
                continue;
            }

            if (ctx.getPackageName().equalsIgnoreCase(task.getTaskInfo().baseActivity.getPackageName()))
                return true;
        }

        return false;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Broadcast to be called from ADB control script or any other activity
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);

        // Load configuration
        config = new AppCustomConfig(this);
        config.loadConfig();

        // Keep screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Event to capture the state of the system bars during user interaction
        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // The system bars are visible.
                            isFullScreen = false;
                        } else {
                            // The system bars are NOT visible.
                            isFullScreen = true;
                        }
                    }
                });

        // VideoView to show animations
        videoView = (VideoView) findViewById(R.id.videoView);

        // TODO Move to performClick
        // Event to hide system bars (or not), when user touches the screen
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isFullScreen) {
                    hideSystemUI();
                }
                return false;
            }
        });

        // To be run when a video is done playing
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                handleVideoCompletion();
            }
        });

        // To be executed when video is ready to be played.
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mp) {
                        if (!videoView.isPlaying()) {
                            videoView.start();
                        }
                    }
                });
            }
        });

        handleSignalState(config.getDefaultState());

        hideSystemUI();

        // Run RosService
        Intent serviceIntent = new Intent(this, SignalStateService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister EventBus and broadcast receiver
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this);
        }
        unregisterReceiver(receiver);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        videoView.resume();
        videoView.seekTo(videoSavedPosition);
    }

    @Override
    protected void onPause() {
        videoView.pause();
        videoSavedPosition = videoView.getCurrentPosition();
        super.onPause();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    /**
     * This callback receives any messages forwarded from the ROS node
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SignalState event) {

        // Get signal name from id
        String state = Utils.getStateNameFromId(event.getState());

        // Convert state string received into a recognizable state
        VideoStateConfig stateConfig = config.getStateConfig(state);

        // Handle signal
        handleSignalState(stateConfig);

    }

    private boolean handleSignalState(VideoStateConfig stateConfig) {

        // Slightly validation for JSON on execution time
        if (stateConfig == null) {
            // Ignore null states
            Toast.makeText(this, "NULL STATE", Toast.LENGTH_LONG).show();
            return false;
        } else if (!stateConfig.isValid()) {
            // Bad state.
            Toast.makeText(this, "INVALID STATE",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        switch (stateConfig.getType()) {
            case VideoStateConfig.TYPE_RUNNER:
                // Ignore runner state. Activity should be launch from service
                Toast.makeText(this, "ALREADY RUNNING ACTIVITY",
                        Toast.LENGTH_LONG).show();

                // Move forward to next state
                String nextState = stateConfig.getNextDefaultState();
                if (nextState != null && !nextState.equals(stateConfig.getRosState())) {
                    // State has next state and it is different from itself
                    VideoStateConfig newState = config.getStateConfig(nextState);

                    // Recursive call
                    if(handleSignalState(newState)) {
                        return true;
                    }
                }
                // If no next state, or it fails to be applied, keep runner state
                updateState(stateConfig);
                break;
            case VideoStateConfig.TYPE_IDLE:
                videoView.stopPlayback();
                videoView.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);
                updateState(stateConfig);
                break;
            case VideoStateConfig.TYPE_LOOP:
            case VideoStateConfig.TYPE_SEQUENTIAL:
                int updateResult = updateState(stateConfig);
                switch (updateResult) {
                    case UPDATE_STATE_CHANGED:
                        playVideo(stateConfig.getLocalVideoName());
                        break;
                    case UPDATE_STATE_QUEUED:
                        Toast.makeText(this, "STATE WAS QUEUED", Toast.LENGTH_LONG).show();
                        break;
                    case UPDATE_STATE_IGNORED:
                        Toast.makeText(this, "STATE WAS IGNORED", Toast.LENGTH_LONG).show();
                        break;
                    case UPDATE_STATE_ERROR:
                        Toast.makeText(this, "STATE ERROR", Toast.LENGTH_LONG).show();
                        break;
                }
                break;
            default:
                Toast.makeText(this, "STATE " + stateConfig.getRosState() +
                                " is a valid ROS state but it was not recognized for the app. " +
                                "Check config file",
                        Toast.LENGTH_LONG).show();
                return false;
        }
        return true;
    }

    private void handleVideoCompletion() {
        // Do we want the video to keep running until a new command is received?
        if (currentStateVideo.getType().equals(VideoStateConfig.TYPE_LOOP)) {
            videoView.start();
        } else if (currentStateVideo.getType().equals(VideoStateConfig.TYPE_SEQUENTIAL)
                && currentStateVideo.getNextDefaultState() != null) {
            // Get next default state to be played next
            VideoStateConfig newState = config.getStateConfig(currentStateVideo.getNextDefaultState());
            handleSignalState(newState);
        }
    }

    private int updateState(VideoStateConfig state) {
        if (state == null) {
            // State not yet supported.
            Toast.makeText(this, "NOT SUPPORTED STATE", Toast.LENGTH_LONG).show();
            return UPDATE_STATE_ERROR;
        }

        if (currentStateVideo == null) {
            // If first execution
            currentStateVideo = new VideoStateConfig();
        }

        if (state.getRosState().equals(currentStateVideo.getRosState())) {
            // We received the same state again. Put it in queue if it is sequential
            if (currentStateVideo.getType().equals(VideoStateConfig.TYPE_SEQUENTIAL)) {
                currentStateVideo.setNextDefaultState(state.getRosState());
                return UPDATE_STATE_QUEUED;
            } else {
                // Ignore otherwise: Recurrent states will be ignored if loop, idle or runner.
                return UPDATE_STATE_IGNORED;
            }
        } else {
            // We received a different state. Let's change it
            currentStateVideo = state;
        }
        return UPDATE_STATE_CHANGED;
    }

    private void playVideo(String videoName) {
        Uri uri;
        // Try to find video on external storage first
        File externalVideo = new File(getExternalFilesDir(null), videoName);
        if (externalVideo.exists()) {
            uri = Uri.fromFile(externalVideo);
        } else {
            // Try to find in already included videos
            int videoID = getResources().getIdentifier(videoName, "raw", this.getPackageName());
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoID);
        }
        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }

        videoView.setVideoURI(uri);
        videoView.start();
    }

    /**
     * This callback receives any messages forwarded from the RosMasterMonitor
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent event) {
        int eventId = event.getMessage();

        if (eventId == MessageEvent.ROS_MASTER_SHOW_UP) {

        } else if (eventId == MessageEvent.ROS_MASTER_WENT_AWAY) {
            // TODO Handle unexpected configuration of sleep state
            if (!currentStateVideo.getType().equals(VideoStateConfig.TYPE_IDLE)
                    && !currentStateVideo.getRosState().equals("SLEEP")) {
                VideoStateConfig newState = config.getStateConfig("SLEEP");
                handleSignalState(newState);
            }
        }
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                //View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                //View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                //| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                //| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        //| View.SYSTEM_UI_FLAG_FULLSCREEN);

        isFullScreen = true;
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private void showSystemUI() {
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

}
