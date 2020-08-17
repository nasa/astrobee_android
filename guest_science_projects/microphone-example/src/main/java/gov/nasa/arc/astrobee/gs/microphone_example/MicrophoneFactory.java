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

// This will launch and manage the microphone ROS node.

package gov.nasa.arc.astrobee.ros.microphone_example;

import gov.nasa.arc.astrobee.AstrobeeException;
import gov.nasa.arc.astrobee.ros.RobotConfiguration;
import gov.nasa.arc.astrobee.ros.NodeExecutorHolder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.node.*;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MicrophoneFactory {
    private final Log logger = LogFactory.getLog(MicrophoneFactory.class);

    private MicrophoneNode m_nodeMain = new MicrophoneNode();

    private final Lock m_lock = new ReentrantLock();
    private final Condition m_changed = m_lock.newCondition();
    private boolean m_running = false;
    private Throwable m_error = null;
    private RobotConfiguration m_robotConfiguration = null;
    private CountDownLatch m_shutdownLatch = null;

    private final class ReadyListener extends DefaultNodeListener {
        @Override
        public void onStart(ConnectedNode connectedNode) {
            m_lock.lock();
            try {
                m_running = true;
                m_error = null;
                m_changed.signalAll();
            } finally {
                m_lock.unlock();
            }
        }

        @Override
        public void onShutdownComplete(Node node) {
            logger.debug("Node shutdown complete");
            m_lock.lock();
            try {
                m_running = false;
                m_changed.signalAll();
            } finally {
                m_lock.unlock();
            }

            if (m_shutdownLatch != null)
                m_shutdownLatch.countDown();
        }

        @Override
        public void onError(final Node node, Throwable throwable) {
            logger.error("Node error", throwable);
            m_lock.lock();
            try {
                m_running = false;
                m_error = throwable;
                m_changed.signalAll();
            } finally {
                m_lock.unlock();
            }

            shutdownNode();
        }
    }

    @SuppressWarnings("unused")
    public MicrophoneFactory() {
        this(new RobotConfiguration());
    }

    public MicrophoneFactory(final RobotConfiguration configuration) {
        m_robotConfiguration = configuration;
        final NodeConfiguration nodeConf = configuration.build();

        final ArrayList<NodeListener> listeners = new ArrayList<>();
        listeners.add(new ReadyListener());
        NodeExecutorHolder.getExecutor().execute(m_nodeMain, nodeConf, listeners);
    }

    public MicrophoneNode getMicrophoneNode() {
        return m_nodeMain;
    }

    void shutdownNode() {
        logger.debug("Attempting to shutdown node");
        m_shutdownLatch = new CountDownLatch(1);
        NodeExecutorHolder.getExecutor().getScheduledExecutorService().submit(
        new Runnable() {
            @Override
            public void run() {
                NodeExecutorHolder.getExecutor().shutdownNodeMain(m_nodeMain);
            }
        }
        );
        NodeExecutorHolder.getLocalExecutor().submit(
        new Runnable() {
            @Override
            public void run() {
                try {
                    if (!m_shutdownLatch.await(5, TimeUnit.SECONDS)) {
                        logger.warn("Node did not shut down in a timely manner, forcing shutdown.");
                        NodeExecutorHolder.shutdownExecutor(1, TimeUnit.SECONDS);
                    }
                }
                catch (InterruptedException e) {
                    logger.debug("Interrupted exception.");
                }
            }
        }
        );
    }

    public void shutdown() {
        shutdownNode();
        NodeExecutorHolder.getLocalExecutor().submit
        (
        new Runnable() {
            @Override
            public void run() {
                NodeExecutorHolder.shutdownExecutor(2, TimeUnit.SECONDS);
            }
        }
        );
    }


}
