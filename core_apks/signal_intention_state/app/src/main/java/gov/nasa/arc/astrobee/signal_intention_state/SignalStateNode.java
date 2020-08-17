
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

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import ff_msgs.SignalState;

public class SignalStateNode extends AbstractNodeMain {

    private static final String TOPIC_SIGNALS = "/signals";

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("signal_state_monitor");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<SignalState> subscriber = connectedNode.newSubscriber(TOPIC_SIGNALS,
                SignalState._TYPE);
        subscriber.addMessageListener(new MessageListener<SignalState>() {
            @Override
            public void onNewMessage(SignalState signalState) {
                // Send state to interface
                EventBus.getDefault().post(signalState);
            }
        });
    }

    @Override
    public void onError(Node node, Throwable throwable) {
        Log.e("ROS_ERROR", "Got an error");
        super.onError(node, throwable);
    }
}
