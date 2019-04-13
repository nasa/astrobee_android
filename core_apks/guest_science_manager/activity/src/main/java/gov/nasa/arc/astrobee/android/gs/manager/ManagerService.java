
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

package gov.nasa.arc.astrobee.android.gs.manager;

import android.content.Intent;
import android.os.IBinder;

import org.ros.android.RosService;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

public class ManagerService extends RosService {

    private static final String NOTIFICATION_TITLE = "GS Manager";
    private static final String NOTIFICATION_TICKER = "Micromanaging your failures in space";
    private static final URI    ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String ROS_HOSTNAME = "hlp";

    private ManagerTimeoutTimer mStartTimer;

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        this.getBaseContext();
        final ManagerNode node = ManagerNode.INSTANCE();
        final NodeConfiguration config = NodeConfiguration.newPublic(ROS_HOSTNAME, ROS_MASTER_URI);
        node.setContext(this.getBaseContext());
        node.setStartTimer(mStartTimer);
        nodeMainExecutor.execute(node, config);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!ACTION_START.equals(intent.getAction())) {
            intent.setAction(ACTION_START);
        }

        putOptExtra(intent, EXTRA_NOTIFICATION_TICKER, NOTIFICATION_TICKER);
        putOptExtra(intent, EXTRA_NOTIFICATION_TITLE, NOTIFICATION_TITLE);

        mStartTimer = new ManagerTimeoutTimer(5000, 2500);

        return super.onStartCommand(intent, flags, startId);
    }

    private static void putOptExtra(Intent intent, String key, String value) {
        if (intent.hasExtra(key))
            return;
        intent.putExtra(key, value);
    }
}
