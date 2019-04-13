
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

package gov.nasa.arc.irg.astrobee.battery_level_display;

import java.io.Serializable;

public class Battery implements Serializable {

    // Constants for battery levels
    public static final String NO_BATTERY = "no_battery";
    public static final String SEARCH_BATTERY = "search_battery";
    public static final String BATTERY_ERROR = "battery_error";
    public static final String BATTERY_EMPTY = "battery_empty"; // 0 - 3
    public static final String BATTERY_5 = "battery_5";         // 4 - 5
    public static final String BATTERY_10 = "battery_10";       // 6 - 10
    public static final String BATTERY_20 = "battery_20";       // 11 - 20
    public static final String BATTERY_30 = "battery_30";       // 21 - 30
    public static final String BATTERY_40 = "battery_40";       // 31 - 40
    public static final String BATTERY_50 = "battery_50";       // 41 - 50
    public static final String BATTERY_60 = "battery_60";       // 51 - 60
    public static final String BATTERY_70 = "battery_70";       // 61 - 70
    public static final String BATTERY_80 = "battery_80";       // 71 - 80
    public static final String BATTERY_90 = "battery_90";       // 81 - 90
    public static final String BATTERY_95 = "battery_95";       // 91 - 95
    public static final String BATTERY_100 = "battery_100";     // 91 - 100

    private String serialNumber;
    private float voltage;
    private float charge;
    private float current;
    private float capacity;
    private float percentage;
    private boolean present;
    private boolean found;
    private String location;

    public Battery() {
        serialNumber = "";
        voltage = 0.0f;
        charge = 0.0f;
        current = 0.0f;
        capacity = 0.0f;
        percentage = 0.0f;
        present = false;
        found = false;
        location = "";
    }

    /**
     * Gets the approximate percentage from the battery level
     *
     * @return String battery_level
     */
    public String getBatteryLevelAprox() {
        if(!this.isFound()) {
            return SEARCH_BATTERY;
        }

        if(this.isPresent()) {
            int percentage = (int) this.getPercentage();
            if (percentage <= 3) {
                return BATTERY_EMPTY;
            } else if (percentage <= 5) {
                return BATTERY_5;
            } else if (percentage <= 10) {
                return BATTERY_10;
            } else if (percentage <= 20) {
                return BATTERY_20;
            } else if (percentage <= 30) {
                return BATTERY_30;
            } else if (percentage <= 40) {
                return BATTERY_40;
            } else if (percentage <= 50) {
                return BATTERY_50;
            } else if (percentage <= 60) {
                return BATTERY_60;
            } else if (percentage <= 70) {
                return BATTERY_70;
            } else if (percentage <= 80) {
                return BATTERY_80;
            } else if (percentage <= 90) {
                return BATTERY_90;
            } else if (percentage <= 95) {
                return BATTERY_95;
            } else if (percentage <= 100) {
                return BATTERY_100;
            } else {
                return BATTERY_ERROR;
            }
        } else {
            return NO_BATTERY;
        }
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public float getVoltage() {
        return voltage;
    }

    public void setVoltage(float voltage) {
        this.voltage = voltage;
    }

    public float getCharge() {
        return charge;
    }

    public void setCharge(float charge) {
        this.charge = charge;
    }

    public float getCapacity() {
        return capacity;
    }

    public void setCapacity(float capacity) {
        this.capacity = capacity;
    }

    public float getPercentage() {
        return percentage;
    }

    public void setPercentage(float percentage) {
        this.percentage = percentage;
    }

    public boolean isPresent() {
        return present;
    }

    public boolean isFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    public void setPresent(boolean present) {
        this.present = present;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public float getCurrent() {
        return current;
    }

    public void setCurrent(float current) {
        this.current = current;
    }
}
