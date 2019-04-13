
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

package gov.nasa.arc.astrobee.cpu_monitor;

import android.app.Service;
import android.content.Context;
import android.util.Log;

import org.ros.internal.node.topic.SubscriberIdentifier;
import org.ros.message.MessageFactory;
import org.ros.message.Time;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.PublisherListener;

import java.util.ArrayList;
import java.util.List;

import ff_msgs.CpuStateStamped;
import ff_msgs.CpuState;
import std_msgs.Header;
import xdroid.toaster.Toaster;

/**
 * ROS Node for publishing data from CPU
 */
public class CpuStatusNode extends AbstractNodeMain {

    // ROS topic name
    public static final String CPU_STATUS_TOPIC = "mgt/cpu_monitor/state";

    // Message factory
    public static MessageFactory msgFactory;

    // CpuStateStamped publisher
    private Publisher<CpuStateStamped> publisher;

    // Node configuration
    private NodeConfiguration mNodeConfig;

    // Activity
    private Context context;

    public CpuStatusNode(Context ctx) {
        this.context = ctx;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("cpu_status_monitor");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        // Create a Message Factory and register a publisher.
        mNodeConfig = NodeConfiguration.newPrivate();
        this.msgFactory = mNodeConfig.getTopicMessageFactory();
        publisher = connectedNode.newPublisher(CPU_STATUS_TOPIC, CpuStateStamped._TYPE);

        // Add a listener for Toast messages.
        publisher.addListener(new PublisherListener<CpuStateStamped>() {
            @Override
            public void onNewSubscriber(Publisher<CpuStateStamped> publisher, SubscriberIdentifier subscriberIdentifier) {
                // When someone is reading messages from this publisher.
                Toaster.toast("CPU Status ROS Publisher has a new SUBSCRIBER");
            }

            @Override
            public void onShutdown(Publisher<CpuStateStamped> publisher) {
                // When the publisher stop`
                Toaster.toast("CPU Status ROS Publisher has STOPPED");
            }

            @Override
            public void onMasterRegistrationSuccess(Publisher<CpuStateStamped> cpuStateStampedPublisher) {
                // When publisher has started
                Toaster.toast("CPU Status ROS Publisher has STARTED");
            }

            @Override
            public void onMasterRegistrationFailure(Publisher<CpuStateStamped> cpuStateStampedPublisher) {
                // Whe publisher has failed on start
                Toaster.toast("CPU Status ROS Publisher registration FAILED");
            }

            @Override
            public void onMasterUnregistrationSuccess(Publisher<CpuStateStamped> cpuStateStampedPublisher) {

            }

            @Override
            public void onMasterUnregistrationFailure(Publisher<CpuStateStamped> cpuStateStampedPublisher) {

            }
        });
    }

    /**
     * Get a CPU java object and put its data into a CpuStateStamped ROS message to published on
     * the ROS node of this object.
     *
     * @param cpu Cpu to publish
     */
    public void publishCpuStateMessage(Cpu cpu) {

        // Create a header
        std_msgs.Header hdr = msgFactory.newFromType(Header._TYPE);
        Time myTime = new Time();
        myTime.secs = 1487370000;
        myTime.nsecs = 0;
        hdr.setStamp(myTime);

        // Create a new CpuStateStamped message
        CpuStateStamped cpuStateStamped = this.publisher.newMessage();

        // Set cpu data into ROS message
        cpuStateStamped.setHeader(hdr);
        cpuStateStamped.setName(cpu.getName());
        cpuStateStamped.setLoadFields(cpu.getLoadFields());
        cpuStateStamped.setAvgLoads(cpu.getAveLoads());
        cpuStateStamped.setTemp(cpu.getTemperature());

        // Set cpu core data into CpuState ROS Messages
        List<CpuState> cpuStates = new ArrayList<>();
        for (CpuCore core : cpu.getCores()) {
            // Create a CpuState message and full it with CpuCore data
            CpuState cpuState = msgFactory.newFromType(CpuState._TYPE);
            cpuState.setEnabled(core.isEnabled());
            cpuState.setLoads(core.getLoads());
            cpuState.setFrequency((int) core.getFrequency());
            cpuState.setMaxFrequency((int) core.getMaxFrequency());
            cpuStates.add(cpuState);
        }

        // Add cpu messages to CpuStateStamped message
        cpuStateStamped.setCpus(cpuStates);

        // Publish message
        publisher.publish(cpuStateStamped);
        Log.i("LOG", "PUBLISH");


    }

    // Getter and Setter section

    public Publisher<CpuStateStamped> getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher<CpuStateStamped> publisher) {
        this.publisher = publisher;
    }


}
