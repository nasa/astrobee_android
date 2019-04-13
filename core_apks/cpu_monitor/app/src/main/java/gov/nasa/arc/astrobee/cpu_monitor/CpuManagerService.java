
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

package gov.nasa.arc.astrobee.cpu_monitor;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.ros.android.RosService;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

/**
 * Monitor and publisher service
 */
public class CpuManagerService extends RosService {

    // Texts for the notification
    private static final String NOTIFICATION_TITLE = "CPU Monitor";
    private static final String NOTIFICATION_TICKER = "CPU State Monitor & Publisher Service";

    // IP Address ROS Master and Hostname
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    // ROS - Android Node
    private CpuStatusNode cpuStatusNode = null;

    boolean isNodeExecuting = false;

    Cpu cpu;

    /*
     * Handler and Runnable for permanent interface updating
     */
    Handler handler;

    private Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if(isNodeExecuting) {
                getAndPublishCpuData();
            }
            handler.postDelayed(refresh, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Create a cpu and fill it with cores
        this.cpu = new Cpu("hlp_cpu_monitor");
        int numberCores = cpu.calculatePhysicalNumberCores();
        //Log.i("LOG", "Number of folders: " + numberCores);

        for(int i = 0; i < numberCores; i++) {
            cpu.addCore(new CpuCore(i));
        }

        // Handler for interface updating
        this.handler = new Handler();
        this.handler.post(refresh);

        // Control messages
        Utils.toastMessage(this, "CPU Service & Publisher RUNNING");
        Log.i("LOG", "ONCREATE FINISHED!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        m_nodeMainExecutorService.stopSelf();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ACTION_START.equals(intent.getAction())) {
            intent.setAction(ACTION_START);
        }

        putOptExtra(intent, EXTRA_NOTIFICATION_TICKER, NOTIFICATION_TICKER);
        putOptExtra(intent, EXTRA_NOTIFICATION_TITLE, NOTIFICATION_TITLE);

        return super.onStartCommand(intent, flags, startId);

    }

    private static void putOptExtra(Intent intent, String key, String value) {
        if (intent.hasExtra(key))
            return;
        intent.putExtra(key, value);
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        cpuStatusNode = new CpuStatusNode(this);

        // Setting configurations for ROS-Android Node
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);

        nodeMainExecutor.execute(cpuStatusNode, nodeConfiguration);
        Log.i("LOG", "NODE EXECUTING!");
        isNodeExecuting = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }


    /**
     * Update cpu data from CPU files and publish it on ROS.
     */
    private void getAndPublishCpuData() {
        cpu.updateCpuData();
        if(cpuStatusNode.getPublisher() != null) {
            cpuStatusNode.publishCpuStateMessage(cpu);
        }
    }
}
