package gov.nasa.arc.astrobee.signal_intention_state;

import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.ros.internal.node.topic.PublisherIdentifier;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.DefaultSubscriberListener;
import org.ros.node.topic.Subscriber;
import org.ros.node.topic.SubscriberListener;

import ff_msgs.SignalState;

/**
 * Created by rgarciar on 5/17/18.
 */

public class SignalStateNode extends AbstractNodeMain {

    private static final String TOPIC_SIGNALS = "/signals";

    Subscriber<SignalState> subscriber;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("signal_state_monitor");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        subscriber = connectedNode.newSubscriber(TOPIC_SIGNALS, SignalState._TYPE);
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
