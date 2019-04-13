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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.VideoView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.lang.reflect.Field;
import java.util.List;

import ff_msgs.SignalState;

public class MainActivity extends Activity {

    public final static String ACTION_STOP = "gov.nasa.arc.astrobee.signal_intention_state.ACTION_STOP";

    private boolean isFullScreen;
    private VideoView videoView;
    private AppCustomConfig config = null;
    private VideoStateConfig currentStateVideo = null;
    private int videoSavedPosition = 0;

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_STOP);
        registerReceiver(receiver, filter);

        config = new AppCustomConfig(this);
        config.loadConfig();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener
                (new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        // Note that system bars will only be "visible" if none of the
                        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
                        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                            // The system bars are visible.
                            //Log.e("Screen", "no full scren");
                            isFullScreen = false;
                        } else {
                            // The system bars are NOT visible.
                            //Log.e("Screen", "full scren");
                            isFullScreen = true;
                        }
                    }
                });

        videoView = (VideoView) findViewById(R.id.videoView);

        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isFullScreen) {
                    hideSystemUI();
                }
                return false;
            }
        });

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (currentStateVideo.isLoop()) {
                    videoView.start();
                }

                if (currentStateVideo.getNextDefaultState() != null) {
                    VideoStateConfig stateConfig = config.getStateConfig(currentStateVideo.getNextDefaultState());
                    if (stateConfig.isAppStopper()) {
                        finish();
                    } else {
                        playVideoSignal(currentStateVideo.getNextDefaultState());
                    }
                }
            }
        });

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

        playVideoSignal(config.getRunnerState().getNextDefaultState());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(SignalState event) {

        byte value = event.getState();
        String state = null;

        Field[] interfaceFields = SignalState.class.getFields();
        for (Field f : interfaceFields) {
            // Get the name of the action using the given value
            if (f.getType() == byte.class) {
                try {
                    if ((byte) f.get(null) == value) {
                        state = f.getName();
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        // Convert state string received into a recognizable state
        VideoStateConfig stateConfig = config.getStateConfig(state);


        // Slightly validation for JSON on execution time
        if (stateConfig == null) {
            // Ignore null states
            Toast.makeText(this, "NOT SUPPORTED STATE", Toast.LENGTH_LONG).show();
        } else if (stateConfig.isAppRunner()) {
            // Ignore runner state. Activity should be launch from service
            Toast.makeText(this, "Received video on but activity is already running", Toast.LENGTH_LONG).show();
        } else if (stateConfig.isAppStopper()) {
            // Stop app
            finish();
        } else if (stateConfig.getLocalVideoName() != null) {
            if ((stateConfig.getNextDefaultState() == null && !stateConfig.isLoop())
                    || (stateConfig.getNextDefaultState() != null && stateConfig.isLoop())) {
                // Bad config.
                Toast.makeText(this, "BAD CONFIG. State could be categorized as sequential or looper",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // It is a sequential or lopper state. It has a video. Play it.
            playVideoSignal(state);
        } else {
            // Bad formatted state in config.
            Toast.makeText(this, "STATE " + state +
                            " is a valid ROS state but it was unrecognized for the app. Check config file",
                    Toast.LENGTH_LONG).show();
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
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);

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

    private void playVideoSignal(String state) {
        VideoStateConfig videoStateConfig = config.getStateConfig(state);

        if (videoStateConfig == null) {
            // State not yet supported
            // TODO Show a default video or something else
            Toast.makeText(this, "NOT SUPPORTED STATE", Toast.LENGTH_LONG).show();
            Log.e("LOG", "No video found");
            return;
        }

        if (currentStateVideo == null) {
            // First execution
            currentStateVideo = new VideoStateConfig();
        }

        if (videoStateConfig.getRosState().equals(currentStateVideo.getRosState())) {
            // We received the same state again. Put it in queue
            if (!currentStateVideo.isLoop()) {
                currentStateVideo.setNextDefaultState(videoStateConfig.getRosState());
            }
        } else {
            // We received a different state. Let's change it
            String videoName = videoStateConfig.getLocalVideoName();
            playVideo(videoName);

            currentStateVideo = videoStateConfig;
        }
    }

    private void playVideo(String videoName) {
        int videoID = getResources().getIdentifier(videoName, "raw", this.getPackageName());
        Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoID);

        if (videoView.isPlaying()) {
            videoView.stopPlayback();
        }

        videoView.setVideoURI(uri);
        videoView.start();
    }

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

}
