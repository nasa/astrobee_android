

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

package gov.nasa.arc.irg.astrobee.rosexample;

import android.util.Log;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

class SimpleNode extends AbstractNodeMain implements MessageListener<std_msgs.String> {
    private static final String TAG = "SimpleNode";

    @Override
    public void onNewMessage(std_msgs.String msg) {
        OnMessageListener listener = mListener;
        if (listener == null) {
            return;
        }

        try {
            listener.onMessage(msg.getData());
        } catch (Exception ex) {
            Log.e(TAG, "OnMessageListener threw an exception", ex);
        }
    }

    interface OnMessageListener {
        void onMessage(final String msg);
    }

    private OnMessageListener mListener = null;
    private Publisher<std_msgs.String> mPublisher;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("simple_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Publisher<std_msgs.String> publisher = connectedNode.newPublisher(
                getDefaultNodeName().join("out"),
                std_msgs.String._TYPE);
        mPublisher = publisher;

        Subscriber<std_msgs.String> subscriber = connectedNode.newSubscriber(
                getDefaultNodeName().join("in"),
                std_msgs.String._TYPE);
        subscriber.addMessageListener(this);
    }

    public void sendMessage(final String msg) {
        if (mPublisher == null) {
            return;
        }

        std_msgs.String strMsg = mPublisher.newMessage();
        strMsg.setData(msg);
        mPublisher.publish(strMsg);
    }

    public void setListener(OnMessageListener listener) {
        mListener = listener;
    }
}
