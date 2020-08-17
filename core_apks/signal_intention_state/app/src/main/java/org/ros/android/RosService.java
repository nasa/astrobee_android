
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

package org.ros.android;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.IBinder;

import org.ros.node.NodeMainExecutor;

public abstract class RosService extends Service {
    public static final String ACTION_START = NodeMainExecutorService.ACTION_START;
    public static final String EXTRA_NOTIFICATION_TICKER = NodeMainExecutorService.EXTRA_NOTIFICATION_TICKER;
    public static final String EXTRA_NOTIFICATION_TITLE = NodeMainExecutorService.EXTRA_NOTIFICATION_TITLE;
    public static final String EXTRA_MASTER_URI = "org.ros.android.EXTRA_MASTER_URI";
    public static final String EXTRA_HOSTNAME = "org.ros.android.EXTRA_HOSTNAME";

    private final NodeMainExecutorServiceConnection m_nodeMainExecutorServiceConnection =
            new NodeMainExecutorServiceConnection();
    protected NodeMainExecutorService m_nodeMainExecutorService;

    protected abstract void init(NodeMainExecutor nodeMainExecutor);

    String m_notificationTicker = null;
    String m_notificationTitle = null;
    protected boolean isExecutorServiceRunning = false;

    private final class NodeMainExecutorServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            m_nodeMainExecutorService = ((NodeMainExecutorService.LocalBinder) binder).getService();
            //init();
            isExecutorServiceRunning = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }

    }

    protected void init() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                init(m_nodeMainExecutorService);
                return null;
            }
        }.execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START)) {
            final String uri = intent.getStringExtra(EXTRA_MASTER_URI);
            m_notificationTicker = intent.getStringExtra(EXTRA_NOTIFICATION_TICKER);
            m_notificationTitle = intent.getStringExtra(EXTRA_NOTIFICATION_TITLE);
            bindNodeExecutorService();
        }
        return START_STICKY;
    }

    protected void bindNodeExecutorService() {
        Intent intent = new Intent(this, NodeMainExecutorService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_NOTIFICATION_TICKER, m_notificationTicker);
        intent.putExtra(EXTRA_NOTIFICATION_TITLE, m_notificationTitle);
        startService(intent);
        bindService(intent, m_nodeMainExecutorServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(m_nodeMainExecutorServiceConnection);
    }
}
