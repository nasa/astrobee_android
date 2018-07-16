
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

import org.ros.address.BindAddress;
import org.ros.address.InetAddressFactory;
import org.ros.exception.RosRuntimeException;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.NodeConfiguration;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RobotConfiguration {
    private URI m_masterUri = null;
    private String m_host = null;
    private String m_nodeName = null;
    private String m_robotName = null;

    private int m_tcpPort = 0;
    private int m_rpcPort = 0;

    private final Map<String, String> m_environment;
    private final Map<GraphName, GraphName> m_remappings;

    public RobotConfiguration() {
        this(System.getenv());
    }

    public RobotConfiguration(final Map<String,String> environment) {
        m_environment = environment;
        m_remappings = new HashMap<>();
    }

    public NodeConfiguration build() {
        final NodeConfiguration config = NodeConfiguration.newPublic(getHost());
        config.setMasterUri(getMasterUri());
        config.setParentResolver(buildParentResolver());
        config.setRosRoot(null);
        config.setRosPackagePath(getRosPackagePath());
        if (m_nodeName != null) {
            config.setNodeName(m_nodeName);
        }
        if (m_tcpPort > 0) {
            config.setTcpRosBindAddress(BindAddress.newPublic(m_tcpPort));
        }
        if (m_rpcPort > 0) {
            config.setXmlRpcBindAddress(BindAddress.newPublic(m_rpcPort));
        }
        return config;
    }

    private String getHost() {
        if (m_host != null)
            return m_host;

        if (m_environment.containsKey(org.ros.EnvironmentVariables.ROS_IP)) {
            m_host = m_environment.get(org.ros.EnvironmentVariables.ROS_IP);
        } else if (m_environment.containsKey(org.ros.EnvironmentVariables.ROS_HOSTNAME)) {
            m_host = m_environment.get(org.ros.EnvironmentVariables.ROS_HOSTNAME);
        } else {
            m_host = InetAddressFactory.newLoopback().getHostAddress();
        }

        return m_host;
    }

    private URI getMasterUri() {
        if (m_masterUri != null )
            return m_masterUri;

        try {
            if (m_environment.containsKey(org.ros.EnvironmentVariables.ROS_MASTER_URI)) {
                m_masterUri = new URI(m_environment.get(org.ros.EnvironmentVariables.ROS_MASTER_URI));
            } else {
                m_masterUri = NodeConfiguration.DEFAULT_MASTER_URI;
            }
            return m_masterUri;
        } catch (URISyntaxException e) {
            throw new RosRuntimeException("Invalid master URI: " + m_masterUri);
        }
    }

    private NameResolver buildParentResolver() {
        GraphName namespace = GraphName.root();
        if (m_environment.containsKey(org.ros.EnvironmentVariables.ROS_NAMESPACE)) {
            namespace = GraphName.of(m_environment.get(org.ros.EnvironmentVariables.ROS_NAMESPACE)).toGlobal();
        }
        return new NameResolver(namespace, m_remappings);
    }

    private List<File> getRosPackagePath() {
        if (m_environment.containsKey(org.ros.EnvironmentVariables.ROS_PACKAGE_PATH)) {
            String rosPackagePath = m_environment.get(org.ros.EnvironmentVariables.ROS_PACKAGE_PATH);
            List<File> paths = new ArrayList<>();
            for (String path : rosPackagePath.split(File.pathSeparator)) {
                paths.add(new File(path));
            }
            return paths;
        } else {
            return new ArrayList<>();
        }
    }

    public String getRobotName() {
        if (m_robotName != null)
            return m_robotName;
        if (m_environment.containsKey(EnvironmentVariables.ASTROBEE_ROBOT)) {
            m_robotName = m_environment.get(EnvironmentVariables.ASTROBEE_ROBOT);
        }
        return m_robotName;
    }

    public RobotConfiguration setRobotName(final String name) {
        m_robotName = name;
        return this;
    }

    public RobotConfiguration setHostname(final String hostname) {
        m_host = hostname;
        return this;
    }

    public RobotConfiguration setTcpPort(int port) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port must be between 0 and 65535");
        m_tcpPort = port;
        return this;
    }

    public RobotConfiguration setRpcPort(int port) {
        if (port < 0 || port > 0xFFFF)
            throw new IllegalArgumentException("port must be between 0 and 65535");
        m_rpcPort = port;
        return this;
    }

    public RobotConfiguration setMasterUri(final URI uri) {
        m_masterUri = uri;
        return this;
    }

    public RobotConfiguration setNodeName(final String name) {
        m_nodeName = name;
        return this;
    }
}
