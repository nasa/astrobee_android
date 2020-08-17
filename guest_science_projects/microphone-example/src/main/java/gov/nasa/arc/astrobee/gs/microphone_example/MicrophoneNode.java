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

// A ROS node that acts like a microphone. It publishes the sound intensity
// at a given location as a marker array to be visualized in RVIZ.

package gov.nasa.arc.astrobee.ros.microphone_example;

import java.util.Vector;

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
import org.ros.message.Time;
import org.ros.message.Duration;

import ff_msgs.AckStamped;
import ff_msgs.CommandStamped;
import geometry_msgs.Point;
import geometry_msgs.Pose;
import geometry_msgs.Quaternion;
import geometry_msgs.Vector3;
import std_msgs.ColorRGBA;
import std_msgs.Header;
import visualization_msgs.Marker;
import visualization_msgs.MarkerArray;

import gov.nasa.arc.astrobee.AstrobeeRuntimeException;
import gov.nasa.arc.astrobee.PendingResult;

public class MicrophoneNode extends AbstractNodeMain implements MessageListener<AckStamped> {
    private final Log logger = LogFactory.getLog(MicrophoneNode.class);

    private ConnectedNode m_node = null;
    private Publisher<MarkerArray> m_gsDataPublisher = null;
    private boolean m_ready = false;
    private static int m_meas_id = 0;

    @Override
    public synchronized void onStart(final ConnectedNode connectedNode) {
        m_node = connectedNode;
        m_gsDataPublisher = connectedNode.newPublisher("gs/microphone", MarkerArray._TYPE);
    }

    @Override
    public synchronized void onNewMessage(final AckStamped ack) {
    }

    @Override
    public synchronized void onShutdown(Node node) {
        m_node = null;
        m_gsDataPublisher = null;
    }

    synchronized MessageFactory getTopicMessageFactory() {
        if (m_node == null)
            throw new AstrobeeRuntimeException("Node is not ready or died");
        return m_node.getTopicMessageFactory();
    }

    // extracted from https://github.com/OctoMap/octomap_mapping
    // Assign a color to each height. The heights should be between
    // 0 and 1 to get unique colors.
    void heightMapColor(double height, std_msgs.ColorRGBA C) {

        C.setA(1.0f);

        // blend over HSV-values (more colors)

        float s = 1.0f;
        float v = 1.0f;

        double height_mod = height;
        height_mod -= Math.floor(height_mod);
        height_mod *= 6;
        int i = (int)Math.floor(height_mod);
        double f = height_mod - i;
        if (i % 2 == 0)
            f = 1.0 - f;  // if i is even

        float m = (float)(v * (1.0 - s));
        float n = (float)(v * (1.0 - s * f));

        switch (i) {
            case 6:
            case 0:
                C.setR(v); C.setG(n); C.setB(m);
                break;
            case 1:
                C.setR(n); C.setG(v); C.setB(m);
                break;
            case 2:
                C.setR(m); C.setG(v); C.setB(n);
                break;
            case 3:
                C.setR(m); C.setG(n); C.setB(v);
                break;
            case 4:
                C.setR(n); C.setG(m); C.setB(v);
                break;
            case 5:
                C.setR(v); C.setG(m); C.setB(n);
                break;
            default:
                C.setR(1.0f); C.setG(0.5f); C.setB(0.5f);
                break;
        }
    }

    synchronized void publishSound(java.util.Vector ps) {

        if (ps.size() != 4) {
            logger.error("Wrong size passed in.");
            return;
        }

        // Will instantiate objects from here
        MessageFactory mf = m_node.getTopicMessageFactory();

        // Form a marker
        Marker M = mf.newFromType(Marker._TYPE);

        // Header
        std_msgs.Header hdr = mf.newFromType(Header._TYPE);
        Time T = Time.fromMillis(System.currentTimeMillis());
        hdr.setStamp(T);
        hdr.setFrameId("world");
        M.setHeader(hdr);

        // Properties
        // Each marker must have an unique id
        M.setId(m_meas_id); m_meas_id++;
        M.setType(Marker.CUBE_LIST);
        M.setNs("thick_traj");
        //M.setLifetime(new Duration(1000000));
        M.setAction(Marker.ADD);
        M.setText("Microphone");

        // Scale
        Vector3 scale = mf.newFromType(Vector3._TYPE);
        scale.setX(0.05);
        scale.setY(0.05);
        scale.setZ(0.05);
        M.setScale(scale);

        // Position
        geometry_msgs.Point point = mf.newFromType(geometry_msgs.Point._TYPE);
        point.setX((double)ps.get(0));
        point.setY((double)ps.get(1));
        point.setZ((double)ps.get(2));

        // Use identity for orientation
        Quaternion orientation = mf.newFromType(Quaternion._TYPE);
        orientation.setW(1.0); // Use the identity orientation

        // Populate and set the pose
        Pose pose = mf.newFromType(Pose._TYPE);
        //pose.setPosition(point);  // To be set later via setPoints()
        pose.setOrientation(orientation);
        M.setPose(pose);

        // Color
        std_msgs.ColorRGBA C = mf.newFromType(std_msgs.ColorRGBA._TYPE);
        heightMapColor((double)ps.get(3), C);
        M.setColor(C);

        // Set the points to plot
        java.util.List<geometry_msgs.Point> pList
            = new java.util.ArrayList<geometry_msgs.Point>();
        pList.add(point);
        M.setPoints(pList);

        // Set the colors to plot
        java.util.List<std_msgs.ColorRGBA> cList
            = new java.util.ArrayList<std_msgs.ColorRGBA>();
        cList.add(C);
        M.setColors(cList);


        //void setPoints(java.util.List<geometry_msgs.Point> value);
        //java.util.List<std_msgs.ColorRGBA> getColors();
        //void setColors(java.util.List<std_msgs.ColorRGBA> value);
        java.util.List<visualization_msgs.Marker> mList =
            new java.util.ArrayList<visualization_msgs.Marker>();
        mList.add(M);

        // Form a marker array
        MarkerArray MA = mf.newFromType(MarkerArray._TYPE);
        MA.setMarkers(mList);
        m_gsDataPublisher.publish(MA);
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("Microphone");
    }
}
