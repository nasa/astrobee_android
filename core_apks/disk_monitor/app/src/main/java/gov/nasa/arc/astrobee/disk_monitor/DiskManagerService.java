
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

package gov.nasa.arc.astrobee.disk_monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import org.ros.android.RosService;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;
import java.util.ArrayList;

import xdroid.toaster.Toaster;

public class DiskManagerService extends RosService {

    public class MyBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            reloadStorage();
        }
    }

    // Texts for the notification
    private static final String NOTIFICATION_TITLE = "DISK Monitor";
    private static final String NOTIFICATION_TICKER = "DISK State Monitor & Publisher Service";

    // IP Address ROS Master and Hostname
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    // ROS - Android Node
    private DiskStateNode diskStateNode = null;

    // Broascast receiver for media events
    BroadcastReceiver br;

    boolean isNodeExecuting = false;

    Storage storage;

    /*
     * Handler and Runnable for permanent interface updating
     */
    Handler handler;

    private Runnable refresh = new Runnable() {
        @Override
        public void run() {
            if(isNodeExecuting) {
                getAndPublishDiskData();
            }
            handler.postDelayed(refresh, 1000);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO Detect -somehow- new adopted storage on running time

        // Create and configure a broadcast receiver.
        br = new MyBroadcastReceiver();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addDataScheme("file");
        this.registerReceiver(br, filter);

        // Detect storage devices on this android terminal
        loadStorage();

        // Handler for interface updating
        this.handler = new Handler();
        this.handler.post(refresh);

        // Control messages
        Toaster.toast("DISK Service & Publisher RUNNING");
        Log.i("LOG", "ONCREATE FINISHED!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Stop services, handler and broadcast receiver.
        m_nodeMainExecutorService.stopSelf();
        handler.removeCallbacksAndMessages(null);
        unregisterReceiver(br);
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
        diskStateNode = new DiskStateNode();

        // Setting configurations for ROS-Android Node
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);

        nodeMainExecutor.execute(diskStateNode, nodeConfiguration);
        Log.i("LOG", "NODE EXECUTING!");
        isNodeExecuting = true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void reloadStorage() {
        handler.removeCallbacksAndMessages(null);
        loadStorage();
        handler.post(refresh);
    }

    private void loadStorage() {
        // Create a general storage and fill it with disks
        storage = new Storage("hlp");
        ArrayList<Disk> disks = new ArrayList<>();

        // Reading external and adopted paths on file system.
        ArrayList<String> externalPaths = DiskUtils.getMountedPaths(DiskUtils.EXTERNAL_MEMORY);
        ArrayList<String> adoptedPaths = DiskUtils.getMountedPaths(DiskUtils.ADOPTED_INTERNAL_MEMORY);

        // Create internal disk -always present.
        Disk internal = new Disk("Internal", DiskUtils.BUILT_IN_INTERNAL_MEMORY, DiskUtils.BUILT_IN_INTERNAL_PATH);
        disks.add(internal);

        // Create external and adopted disks, if exist.
        for (String path : externalPaths) {
            Disk d = new Disk("External", DiskUtils.EXTERNAL_MEMORY, path);
            disks.add(d);
        }

        for (String path : adoptedPaths) {
            Disk d = new Disk("Adopted", DiskUtils.ADOPTED_INTERNAL_MEMORY, path);
            disks.add(d);
        }

        // Set disks to storage.
        storage.setDisks(disks);
    }

    /**
     * Update disk data and publish it on ROS.
     */
    private void getAndPublishDiskData() {
        if(storage.updateData()) {
            if(diskStateNode.getPublisher() != null) {
                diskStateNode.publishDiskStateMessage(storage);
            }
        } else {
            loadStorage();
        }
    }
}
