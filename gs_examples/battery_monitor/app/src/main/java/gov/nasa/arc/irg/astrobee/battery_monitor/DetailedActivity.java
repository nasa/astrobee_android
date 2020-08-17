
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

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.commons.lang.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.arc.irg.astrobee.battery_monitor.types.Battery;

public class DetailedActivity extends Activity{

    Battery battery = null;
    TextView tvBatteryLocation;
    ImageView ivBatteryLevel;
    TextView tvBatteryPercentage;
    ListView lvBatteryDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detailed);

        // Getting battery data from Main Activity
        battery = (Battery) getIntent().getSerializableExtra("Battery");

        // Setting views and updating
        tvBatteryLocation = (TextView) findViewById(R.id.tv_battery_location);
        ivBatteryLevel = (ImageView) findViewById(R.id.iv_battery_level);
        tvBatteryPercentage = (TextView) findViewById(R.id.tv_battery_percentage);
        lvBatteryDetails = (ListView) findViewById(R.id.lv_battery_details);

        // TODO (Ruben) Make accessible data from ROS-Android node for other Activities.
        // TODO (Ruben) Use a handler to update data in a continuous way
        updateUI();
    }

    /**
     * Method that updates the interface
     */
    protected void updateUI() {

        // Making location more readable
        String location = StringUtils.replace(battery.getLocation(), "_", " ");

        // Displaying data from the battery
        if (battery != null) {
            tvBatteryLocation.setText(location);
            tvBatteryPercentage.setText((int) battery.getPercentage() + "%");
            lvBatteryDetails.setAdapter(createAdapterForList());

            displayBatteryLevelSimple(battery, ivBatteryLevel, tvBatteryPercentage);
        } else {
            Toast.makeText(this, R.string.error_no_battery, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Simple method that displays an image suitable to the current battery level.
     *
     * @param battery Battery object.
     * @param display Java ImageView object.
     * @param textPercentage Java TextView object.
     */
    protected void displayBatteryLevelSimple(Battery battery, ImageView display, TextView textPercentage) {
        String batteryLevelAprox = battery.getBatteryLevelAprox();
        Context c = this;

        if(battery.isPresent()) {
            String percentage = (int) battery.getPercentage() + "%";
            textPercentage.setText(percentage);
        } else {
            textPercentage.setText("No Battery");
        }

        int color = 0;

        switch (batteryLevelAprox){
            case "battery_5":
                color = ContextCompat.getColor(this, R.color.colorOrangeBattery);
                break;
            case "battery_10":
            case "battery_20":
                color = ContextCompat.getColor(this, R.color.colorOrangeBattery);
                break;
            case "battery_30":
            case "battery_40":
            case "battery_50":
            case "battery_60":
            case "battery_70":
            case "battery_80":
            case "battery_90":
            case "battery_95":
            case "battery_100":
                color = ContextCompat.getColor(this, R.color.colorGreenBattery);
                break;
            default:
                color = ContextCompat.getColor(this, R.color.colorBlueBattery);
                break;
        }

        textPercentage.setTextColor(color);


        display.setImageResource(getResources().getIdentifier("drawable/round_" + batteryLevelAprox, null, c.getPackageName()));

    }

    /**
     * Method that creates a SimpleAdapter for ListView Details
     * @return SimpleAdapter
     */
    private SimpleAdapter createAdapterForList() {

        // List for SimpleAdapter
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();

        // Creating associated maps for the previous list. Each map will be an item in the ListView.
        Map<String, String> datam1 = new HashMap<>(2);
        datam1.put("title", "SERIAL NUMBER");
        datam1.put("value", battery.getSerialNumber());

        Map<String, String> datam2 = new HashMap<>(2);
        datam2.put("title", "VOLTAGE");
        datam2.put("value", battery.getVoltage() + " V");

        Map<String, String> datam3 = new HashMap<>(2);
        datam3.put("title", "CURRENT");
        datam3.put("value", battery.getCurrent() + " A");

        Map<String, String> datam4 = new HashMap<>(2);
        datam4.put("title", "CHARGE");
        datam4.put("value", battery.getCharge() + " A");

        Map<String, String> datam5 = new HashMap<>(2);
        datam5.put("title", "CAPACITY");
        datam5.put("value", battery.getCapacity() + " A");

        // Adding maps to list.
        data.add(datam1);
        data.add(datam2);
        data.add(datam3);
        data.add(datam4);
        data.add(datam5);

        // Adapter for data displaying on ListView
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2,
                new String[]{"title", "value"}, new int[] {android.R.id.text1, android.R.id.text2});

        return adapter;
    }
}
