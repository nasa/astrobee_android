
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

package gov.nasa.arc.astrobee.ros;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.node.DefaultNodeMainExecutor;
import org.ros.node.NodeMainExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public final class NodeExecutorHolder {
    private static final Log logger = LogFactory.getLog(NodeExecutorHolder.class);

    static final class NamedThreadFactory implements ThreadFactory {
        private final ThreadGroup m_group;
        private final String m_name;
        private boolean m_daemon;

        NamedThreadFactory(final String name, boolean daemon) {
            SecurityManager s = System.getSecurityManager();
            m_group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            m_name = name;
            m_daemon = daemon;
        }

        @Override
        public Thread newThread(final Runnable r) {
            final Thread t = new Thread(m_group, r, m_name, 0);
            t.setDaemon(m_daemon);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    };

    private static NodeMainExecutor s_executor = null;
    private static ExecutorService s_local = null;

    public static NodeMainExecutor getExecutor() {
        if (s_local == null) {
            s_local = Executors.newSingleThreadExecutor(new NamedThreadFactory("NodeMain Local", true));
        }

        if (s_executor == null)
            s_executor = DefaultNodeMainExecutor.newDefault();
        return s_executor;
    }

    // TODO(Katie/Ted) Remove public when we merge Robbie's branch into master
    public static ExecutorService getLocalExecutor() {
        return s_local;
    }

    // TODO(Katie/Ted) Remove public when we merge Robbie's branch into master
    public static void shutdownExecutor(long time, TimeUnit units) {
        if (s_executor == null)
            return;
        logger.info("Attempting to shutdown ROS executor service.");
        s_executor.getScheduledExecutorService().shutdown();
        try {
            logger.info("Waiting " + time + " " + units + " for termination");
            if (!s_executor.getScheduledExecutorService().awaitTermination(time, units)) {
                logger.warn("ROS did not shut down in a timely manner, forcing shut down.");
                s_executor.getScheduledExecutorService().shutdownNow();
            }
        } catch (InterruptedException e) { }
        s_executor = null;
    }
}
