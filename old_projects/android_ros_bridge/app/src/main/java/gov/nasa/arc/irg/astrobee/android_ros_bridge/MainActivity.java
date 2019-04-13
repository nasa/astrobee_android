
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

package gov.nasa.arc.irg.astrobee.android_ros_bridge;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

public class MainActivity extends RosActivity implements View.OnClickListener {
    private static final URI ROS_MASTER_URI = URI.create("http://10.0.3.1:11311");

    // UI Widgets
    private Button mbtn_startService, mbtn_stopService;
    // UI thread handler
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    // ROS Node
    private GuestScienceRosMessages guestScienceDemoNode = null;


    public MainActivity() {
        super("ROS Example", "Example ROS node", ROS_MASTER_URI);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mbtn_startService = (Button) findViewById(R.id.btn_startService);
        mbtn_stopService = (Button) findViewById(R.id.btn_stopService);

        mbtn_startService.setOnClickListener(this);
        mbtn_stopService.setOnClickListener(this);


        startService(new Intent(getBaseContext(), AndroidRosBridgeService.class));
    }

    @Override
    protected void init(NodeMainExecutor node) {
        guestScienceDemoNode = GuestScienceRosMessages.getSingletonInstance();
        guestScienceDemoNode.setListener(new GuestScienceRosMessages.OnMessageListener() {
            @Override
            public void onMessage(final String msg) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        AndroidRosBridgeService.setMsgRobotToService(msg);
                    }
                });
            }
        });

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic("10.0.3.15");
        nodeConfiguration.setMasterUri(getMasterUri());

        node.execute(guestScienceDemoNode, nodeConfiguration);
    }

    @Override
    public void onClick(View v) {
        if(v.equals(mbtn_stopService)) {
            doStopService();
        } else if(v.equals(mbtn_startService)) {
            doStartService();
        }
    }

    public void doStartService() {
        startService(new Intent(getBaseContext(), AndroidRosBridgeService.class));
    }

    public void doStopService() {
        stopService(new Intent(getBaseContext(), AndroidRosBridgeService.class));
    }
}
