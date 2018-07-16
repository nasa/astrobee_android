

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

package gov.nasa.arc.irg.astrobee.moveexample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.TextSwitcher;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;

import java.net.URI;

public class MainActivity extends RosActivity {
    private static final URI ROS_MASTER_URI = URI.create("http://10.42.0.100:11311");

    // UI Widgets
    protected EditText mSendText;
    protected TextSwitcher mRecvText;

    // UI thread handler
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    // ROS Node
    private SimpleNode mSimpleNode = null;

    public MainActivity() {
        super("ROS Example", "Example ROS node", ROS_MASTER_URI);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSendText = (EditText) findViewById(R.id.main_text_input);
        mRecvText = (TextSwitcher) findViewById(R.id.main_text_recv);
    }

    public void onBtnSendClicked(View v) {
        mSimpleNode.sendMessage();
    }

    @Override
    protected void init(NodeMainExecutor node) {
        mSimpleNode = new SimpleNode();
        mSimpleNode.setListener(new SimpleNode.OnMessageListener() {
            @Override
            public void onMessage(final String msg) {
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRecvText.setText(msg);
                    }
                });
            }
        });

        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(getRosHostname());
        nodeConfiguration.setMasterUri(getMasterUri());

        node.execute(mSimpleNode, nodeConfiguration);
    }

}
