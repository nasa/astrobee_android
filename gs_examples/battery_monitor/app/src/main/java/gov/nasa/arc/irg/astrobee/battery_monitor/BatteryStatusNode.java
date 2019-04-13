
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

package gov.nasa.arc.irg.astrobee.battery_monitor;

import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;
import android.util.Log;

import gov.nasa.arc.irg.astrobee.battery_monitor.types.Battery;
import sensor_msgs.BatteryState;


public class BatteryStatusNode extends AbstractNodeMain {

    public static BatteryStatusNode instance = null;

    // Battery location constants
    static final String LOCATION_TOP_LEFT = "TOP_LEFT";
    static final String LOCATION_TOP_RIGHT = "TOP_RIGHT";
    static final String LOCATION_BOTTOM_LEFT = "BOTTOM_LEFT";
    static final String LOCATION_BOTTOM_RIGHT = "BOTTOM_RIGHT";
    static final String LOGTAG = "BATTERY_MONITOR";

    // Battery state ROS Topic names
    static final String BATTERY_TOP_LEFT_STATE_TOPIC = "/hw/eps/battery/top_left/state";
    static final String BATTERY_TOP_RIGHT_STATE_TOPIC = "/hw/eps/battery/top_right/state";
    static final String BATTERY_BOTTOM_LEFT_STATE_TOPIC = "/hw/eps/battery/bottom_left/state";
    static final String BATTERY_BOTTOM_RIGHT_STATE_TOPIC = "/hw/eps/battery/bottom_right/state";

    Battery batteryTopLeft = new Battery();
    Battery batteryTopRight = new Battery();
    Battery batteryBottomLeft = new Battery();
    Battery batteryBottomRight = new Battery();

    Subscriber<BatteryState> topLeftSubscriber;
    Subscriber<BatteryState> topRightSubscriber;
    Subscriber<BatteryState> bottomLeftSubscriber;
    Subscriber<BatteryState> bottomRightSubscriber;

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("battery_status_monitor");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        // Setting location by default
        batteryTopLeft.setLocation(LOCATION_TOP_LEFT);
        batteryTopRight.setLocation(LOCATION_TOP_RIGHT);
        batteryBottomLeft.setLocation(LOCATION_BOTTOM_LEFT);
        batteryBottomRight.setLocation(LOCATION_BOTTOM_RIGHT);

        // Creating subscribers for ROS Topics and adding message listeners
        topLeftSubscriber = connectedNode.newSubscriber(
                BATTERY_TOP_LEFT_STATE_TOPIC, BatteryState._TYPE);
        topLeftSubscriber.addMessageListener(new MessageListener<BatteryState>() {
            @Override
            public void onNewMessage(BatteryState batteryState) {
                batteryTopLeft = updateBattery(batteryState);
                instance = BatteryStatusNode.this;
                Log.i("LOG", "MSG TOP LEFT");
            }
        }, 10);

        topRightSubscriber = connectedNode.newSubscriber(
                BATTERY_TOP_RIGHT_STATE_TOPIC, BatteryState._TYPE);
        topRightSubscriber.addMessageListener(new MessageListener<BatteryState>() {
            @Override
            public void onNewMessage(BatteryState batteryState) {
                batteryTopRight = updateBattery(batteryState);
                instance = BatteryStatusNode.this;
                Log.i("LOG", "MSG TOP RIGHT");
            }
        }, 10);

        bottomLeftSubscriber = connectedNode.newSubscriber(
                BATTERY_BOTTOM_LEFT_STATE_TOPIC, BatteryState._TYPE);
        bottomLeftSubscriber.addMessageListener(new MessageListener<BatteryState>() {
            @Override
            public void onNewMessage(BatteryState batteryState) {
                batteryBottomLeft = updateBattery(batteryState);
                instance = BatteryStatusNode.this;
                Log.i("LOG", "MSG BTM LEFT");
            }
        }, 10);

        bottomRightSubscriber = connectedNode.newSubscriber(
                BATTERY_BOTTOM_RIGHT_STATE_TOPIC, BatteryState._TYPE);
        bottomRightSubscriber.addMessageListener(new MessageListener<BatteryState>() {
            @Override
            public void onNewMessage(BatteryState batteryState) {
                batteryBottomRight = updateBattery(batteryState);
                instance = BatteryStatusNode.this;
                Log.i("LOG", "MSG BTM RIGHT");
            }
        }, 10);

        instance = this;
    }

    /**
     * Method that updates the information about the battery,
     * catching messages received from eps_driver
     *
     * @param batteryState Java Object for sensor_msg/BatteryStatus ROS message
     * @return
     */
    private Battery updateBattery(BatteryState batteryState) {
        Battery battery = new Battery();
        battery.setSerialNumber(batteryState.getSerialNumber());
        battery.setVoltage(batteryState.getVoltage());
        battery.setCharge(batteryState.getCharge());
        battery.setCapacity(batteryState.getCapacity());
        battery.setPercentage(batteryState.getPercentage() * 100);
        battery.setPresent(batteryState.getPresent());
        battery.setFound(true);
        battery.setLocation(batteryState.getLocation());
        battery.setCurrent(batteryState.getCurrent());
        return battery;
    }

    public static BatteryStatusNode getInstance() {
        return instance;
    }

    public Battery[] getBatteries() {
        Battery[] batteries = {batteryTopLeft, batteryTopRight, batteryBottomLeft, batteryBottomRight};
        return batteries;
    }

}
