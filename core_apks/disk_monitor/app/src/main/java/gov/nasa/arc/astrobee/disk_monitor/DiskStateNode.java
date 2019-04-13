
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

package gov.nasa.arc.astrobee.disk_monitor;

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

import ff_msgs.DiskState;
import ff_msgs.DiskStateStamped;
import std_msgs.Header;
import xdroid.toaster.Toaster;

public class DiskStateNode extends AbstractNodeMain {

    // ROS topic name
    public static final String DISK_STATUS_TOPIC = "mgt/disk_monitor/state";

    // Message factory
    public static MessageFactory msgFactory;

    // CpuStateStamped publisher
    private Publisher<DiskStateStamped> publisher;

    // Node configuration
    private NodeConfiguration mNodeConfig;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("disk_status_monitor");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {

        // Create a Message Factory and register a publisher.
        mNodeConfig = NodeConfiguration.newPrivate();
        this.msgFactory = mNodeConfig.getTopicMessageFactory();
        publisher = connectedNode.newPublisher(DISK_STATUS_TOPIC, DiskStateStamped._TYPE);

        // Add a listener for Toast messages.
        publisher.addListener(new PublisherListener<DiskStateStamped>() {
            @Override
            public void onNewSubscriber(Publisher<DiskStateStamped> publisher, SubscriberIdentifier subscriberIdentifier) {
                // When someone is reading messages from this publisher.
                Toaster.toast("DISK Status ROS Publisher has a new SUBSCRIBER");
            }

            @Override
            public void onShutdown(Publisher<DiskStateStamped> publisher) {
                // When the publisher stop`
                Toaster.toast("DISK Status ROS Publisher has STOPPED");
            }

            @Override
            public void onMasterRegistrationSuccess(Publisher<DiskStateStamped> cpuStateStampedPublisher) {
                // When publisher has started
                Toaster.toast("DISK Status ROS Publisher has STARTED");
            }

            @Override
            public void onMasterRegistrationFailure(Publisher<DiskStateStamped> cpuStateStampedPublisher) {
                // Whe publisher has failed on start
                Toaster.toast("DISK Status ROS Publisher registration FAILED");
            }

            @Override
            public void onMasterUnregistrationSuccess(Publisher<DiskStateStamped> cpuStateStampedPublisher) {

            }

            @Override
            public void onMasterUnregistrationFailure(Publisher<DiskStateStamped> cpuStateStampedPublisher) {

            }
        });
    }

    /**
     * Get a Disk java object and put its data into a DiskStateStamped ROS message to published on
     * the ROS node of this object.
     *
     * @param storage Storage to publish
     */
    public void publishDiskStateMessage(Storage storage) {

        // Create a header
        // TODO Ask Katie or Ted if this is the right way to make a header.
        std_msgs.Header hdr = msgFactory.newFromType(Header._TYPE);
        hdr.setStamp(Time.fromMillis(System.currentTimeMillis()));

        DiskStateStamped diskStateStamped = publisher.newMessage();

        diskStateStamped.setHeader(hdr);
        diskStateStamped.setProcessorName(storage.getProcessorName());

        List<DiskState> disks = new ArrayList<>();
        for(Disk d : storage.getDisks()) {
            DiskState ds = msgFactory.newFromType(DiskState._TYPE);
            ds.setPath(d.getLabel());
            ds.setCapacity(d.getCapacity());
            ds.setUsed(d.getUsed());
            disks.add(ds);
        }

        diskStateStamped.setDisks(disks);
        publisher.publish(diskStateStamped);

        Log.i("LOG", "PUBLISH");

    }

    public Publisher<DiskStateStamped> getPublisher() {
        return publisher;
    }

    public void setPublisher(Publisher<DiskStateStamped> publisher) {
        this.publisher = publisher;
    }

}
