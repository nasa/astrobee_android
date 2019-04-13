package gov.nasa.arc.astrobee.signal_intention_state;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.ros.android.RosService;
import org.ros.internal.message.DefaultMessageFactory;
import org.ros.message.MessageFactory;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.lang.reflect.Field;
import java.net.URI;

import ff_msgs.SignalState;

public class SignalStateService extends RosService {

    // IP Address ROS Master
    private static final URI    ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    // Texts for the notification
    private static final String NOTIFICATION_TITLE = "Signal & Intention Monitor";
    private static final String NOTIFICATION_TICKER = "Listening for signal states...";

    private SignalStateNode signalStateNode = null;

    private boolean isNodeExecuting = false;

    private AppCustomConfig config;

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        signalStateNode = new SignalStateNode();

        // Setting configurations for ROS-Android Node
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);

        nodeMainExecutor.execute(signalStateNode, nodeConfiguration);

        Log.i("LOG", "NODE EXECUTING!");
        isNodeExecuting = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        if (intent.getAction() == null || !intent.getAction().equals(ACTION_START)) {
            intent.setAction(ACTION_START);
        }

        putOptExtra(intent, EXTRA_NOTIFICATION_TICKER, NOTIFICATION_TICKER);
        putOptExtra(intent, EXTRA_NOTIFICATION_TITLE, NOTIFICATION_TITLE);

        config = new AppCustomConfig(this);
        config.loadConfig();

        return super.onStartCommand(intent, flags, startId);
    }

    private static void putOptExtra(Intent intent, String key, String value) {
        if (intent.hasExtra(key))
            return;
        intent.putExtra(key, value);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // Stopping service
        m_nodeMainExecutorService.stopSelf();
        EventBus.getDefault().unregister(this);
        Log.i("LOG", "ONDESTROY FINISHED!");
        super.onDestroy();
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

        // Only listen for appRunner state
        if (stateConfig.isAppRunner()) {
            if (!MainActivity.isActivityRunning(this)) {
                // If MainActivity is not running start it
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
            }
        }
    }
}
