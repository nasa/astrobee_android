
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

import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.ros.android.RosService;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

import ff_msgs.SignalState;

public class SignalStateService extends RosService {

    // IP Address ROS Master
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    // Texts for the notification
    private static final String NOTIFICATION_TITLE = "Signal & Intention Monitor";
    private static final String NOTIFICATION_TICKER = "Listening for signal states...";

    private SignalStateNode signalStateNode = null;
    private boolean isNodeExecuting = false;
    private AppCustomConfig config;

    private NodeConfiguration nodeConfiguration;
    private RosMasterMonitor monitor;

    private boolean isNodeStopping = false;


    private void setUpRosValues() {
        // Setting configurations for ROS-Android Node
        nodeConfiguration = NodeConfiguration.newPublic(ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        signalStateNode = new SignalStateNode();

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

        setUpRosValues();

        monitor = new RosMasterMonitor(nodeConfiguration);
        monitor.start();

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
        monitor.stop();
        isNodeExecuting = false;
        Log.i("LOG", "ONDESTROY FINISHED!");
        super.onDestroy();
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

        // Only listen for appRunner state
        if (stateConfig != null && stateConfig.getType().equals(VideoStateConfig.TYPE_RUNNER)) {
            if (!MainActivity.isActivityRunning(this)) {
                // If MainActivity is not running start it
                Intent intent = new Intent(this, MainActivity.class);
                //intent.putExtra("STATE", )
                startActivity(intent);
            }
        }
    }

    /**
     * This callback receives any messages forwarded from the RosMasterMonitor
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMessageEvent(MessageEvent event) {
        int eventId = event.getMessage();

        if (eventId == MessageEvent.ROS_MASTER_SHOW_UP) {
            if (!isNodeExecuting) {
                init();
            } else if (isNodeStopping) {
                SystemClock.sleep(500);
                onMessageEvent(event);
            } else {
                Log.e("ERROR", "ROS Master restarted but node did not");
            }

        } else if (eventId == MessageEvent.ROS_MASTER_WENT_AWAY && isNodeExecuting) {
            isNodeStopping = true;
            m_nodeMainExecutorService.shutdownNodeMain(signalStateNode);
            isNodeExecuting = false;
            isNodeStopping = false;
        }
    }
}
