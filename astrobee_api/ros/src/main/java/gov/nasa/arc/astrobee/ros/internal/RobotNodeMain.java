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

package gov.nasa.arc.astrobee.ros.internal;

import ff_msgs.AckStamped;
import ff_msgs.CommandStamped;
import ff_msgs.EkfState;
import gov.nasa.arc.astrobee.AstrobeeRuntimeException;
import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.ros.DefaultKinematics;
import gov.nasa.arc.astrobee.ros.internal.util.Stringer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.DefaultPublisherListener;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

public class RobotNodeMain extends AbstractNodeMain implements MessageListener<AckStamped> {
    private final Log logger = LogFactory.getLog(RobotNodeMain.class);

    private ConnectedNode m_node = null;
    private Publisher<CommandStamped> m_cmdPublisher = null;
    private boolean m_ready = false;

    private final Map<String, DefaultPendingResult> m_pending = new HashMap<>();
    private final Queue<CommandStamped> m_queue = new LinkedBlockingDeque<>();

    private final Object m_kinematics_lock = new Object();
    private DefaultKinematics m_kinematics = new DefaultKinematics();

    @Override
    public synchronized void onStart(final ConnectedNode connectedNode) {
        m_node = connectedNode;
        m_cmdPublisher = connectedNode.newPublisher("command", CommandStamped._TYPE);
        m_cmdPublisher.addListener(new DefaultPublisherListener<CommandStamped>() {
            @Override
            public void onNewSubscriber(Publisher<CommandStamped> publisher, SubscriberIdentifier subscriberIdentifier) {
                while (!m_queue.isEmpty()) {
                    CommandStamped cmd = m_queue.poll();
                    publisher.publish(cmd);
                }
                synchronized(RobotNodeMain.this) {
                    m_ready = true;
                }
            }
        });
        Subscriber<AckStamped> subscriber = connectedNode.newSubscriber("mgt/ack", AckStamped._TYPE);
        subscriber.addMessageListener(this);

        Subscriber<EkfState> ekfSub = connectedNode.newSubscriber("gnc/ekf", EkfState._TYPE);
        ekfSub.addMessageListener(new MessageListener<EkfState>() {
            @Override
            public void onNewMessage(final EkfState ekfState) {
                synchronized(m_kinematics_lock) {
                    m_kinematics = new DefaultKinematics(ekfState);
                }
            }
        });
    }

    @Override
    public synchronized void onNewMessage(final AckStamped ack) {
        if (!m_pending.containsKey(ack.getCmdId())) {
            logger.warn("Unmatched Ack received: " + Stringer.toString(ack));
            return;
        }

        final DefaultPendingResult pr = m_pending.get(ack.getCmdId());
        logger.debug("Updating status for " + Stringer.toString(pr.getCommand()));
        pr.update(ack);
        if (pr.isFinished()) {
            m_pending.remove(ack.getCmdId());
        }
    }

    @Override
    public synchronized void onShutdown(Node node) {
        m_node = null;
        m_cmdPublisher = null;
    }

    public Kinematics getKinematics() {
        synchronized(m_kinematics_lock) {
            return m_kinematics;
        }
    }

    synchronized MessageFactory getTopicMessageFactory() {
        if (m_node == null)
            throw new AstrobeeRuntimeException("Node is not ready or died");
        return m_node.getTopicMessageFactory();
    }

    synchronized PendingResult publish(CommandStamped cmd) {
        if (m_cmdPublisher == null)
            throw new AstrobeeRuntimeException("Node not ready or dead");

        cmd.getHeader().setStamp(m_node.getCurrentTime());
        DefaultPendingResult pr = new DefaultPendingResult(cmd);

        if (m_ready) {
            logger.debug("Publishing " + Stringer.toString(cmd));
            m_cmdPublisher.publish(cmd);
        } else {
            m_queue.add(cmd);
            pr.setStatus(PendingResult.Status.QUEUED);
        }

        m_pending.put(cmd.getCmdId(), pr);
        return pr;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("gs_node_main");
    }
}
