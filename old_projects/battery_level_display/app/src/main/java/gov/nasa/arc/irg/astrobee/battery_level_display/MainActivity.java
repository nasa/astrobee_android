
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

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.ros.android.RosActivity;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMainExecutor;


import java.net.URI;


public class MainActivity extends RosActivity {

    // IP Address ROS Master
    private static final URI ROS_MASTER_URI = URI.create("http://10.0.3.1:11311");

    // ROS - Android Node
    private BatteryStatusNode batteryStatusNode = null;

    TextView tvTopLeftP;
    TextView tvTopRightP;
    TextView tvBottomLeftP;
    TextView tvBottomRightP;

    ImageView ivTopLeft;
    ImageView ivTopRight;
    ImageView ivBottomLeft;
    ImageView ivBottomRight;

    View.OnClickListener clickListener;


    boolean isNodeExecuting = false;

    /*
     * Handler and Runnable for permanent interface updating
     */
    Handler handler;

    private Runnable refresh = new Runnable() {
        @Override
        public void run() {
            refreshUI();
            handler.postDelayed(refresh, 2000);
        }
    };

    public MainActivity() {
        super("Battery Monitor", "Battery Monitor Service", ROS_MASTER_URI);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTopLeftP = (TextView) findViewById(R.id.tv_tl_level);
        tvTopRightP = (TextView) findViewById(R.id.tv_tr_level);
        tvBottomLeftP = (TextView) findViewById(R.id.tv_bl_level);
        tvBottomRightP = (TextView) findViewById(R.id.tv_br_level);

        ivTopLeft = (ImageView) findViewById(R.id.iv_tl_level);
        ivTopRight = (ImageView) findViewById(R.id.iv_tr_level);
        ivBottomLeft = (ImageView) findViewById(R.id.iv_bl_level);
        ivBottomRight = (ImageView) findViewById(R.id.iv_br_level);

        // Handler for interface updating
        this.handler = new Handler();
        this.handler.post(refresh);

        // Listener for battery details
        clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Battery battery;

                if(view.equals(ivTopLeft)) {
                    battery = batteryStatusNode.batteryTopLeft;
                } else if(view.equals(ivTopRight)) {
                    battery = batteryStatusNode.batteryTopRight;
                } else if(view.equals(ivBottomLeft)) {
                    battery = batteryStatusNode.batteryBottomLeft;
                } else {
                    battery = batteryStatusNode.batteryBottomRight;
                }

                Context c = getApplicationContext();

                if(battery.isPresent()) {
                    Intent intent = new Intent(c, DetailedActivity.class);
                    intent.putExtra("Battery", battery);
                    startActivity(intent);
                } else {
                    Toast.makeText(c, "NO BATTERY", Toast.LENGTH_LONG).show();
                }

            }
        };

        ivTopLeft.setOnClickListener(clickListener);
        ivTopRight.setOnClickListener(clickListener);
        ivBottomLeft.setOnClickListener(clickListener);
        ivBottomRight.setOnClickListener(clickListener);

        Log.i("LOG", "ONCREATE FINISHED!");

    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Stopping service and handler
        nodeMainExecutorService.stopSelf();
        handler.removeCallbacksAndMessages(null);

        Log.i("LOG", "ONDESTROY FINISHED!");
    }

    @Override
    protected void init(NodeMainExecutor nodeMainExecutor) {
        batteryStatusNode = new BatteryStatusNode();

        // Setting configurations for ROS-Android Node
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic("10.0.3.15");
        nodeConfiguration.setMasterUri(getMasterUri());

        nodeMainExecutor.execute(batteryStatusNode, nodeConfiguration);

        Log.i("LOG", "NODE EXECUTING!");
        isNodeExecuting = true;
    }

    /**
     * Method for interface refreshing, It reads the battery information and updates
     * the data on screen
     */
    public void refreshUI() {
        if(isNodeExecuting && this.hasWindowFocus()) {
            Battery btl = batteryStatusNode.batteryTopLeft;
            Battery btr = batteryStatusNode.batteryTopRight;
            Battery bbl = batteryStatusNode.batteryBottomLeft;
            Battery bbr = batteryStatusNode.batteryBottomRight;

            displayBatteryLevelSimple(btl, ivTopLeft, tvTopLeftP);
            displayBatteryLevelSimple(btr, ivTopRight, tvTopRightP);
            displayBatteryLevelSimple(bbl, ivBottomLeft, tvBottomLeftP);
            displayBatteryLevelSimple(bbr, ivBottomRight, tvBottomRightP);

            Log.i("LOG", "UI UPDATED!");
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
        String percentage = "...";

        if(battery.isFound()) {
            percentage = battery.isPresent() ? (int) battery.getPercentage() + "%" : "NP";
        }

        display.setImageResource(getResources().getIdentifier("drawable/" + batteryLevelAprox, null, c.getPackageName()));

        textPercentage.setText(percentage);
    }

    /**
     * Alternative method for displayBatteryLevelSimple.
     *
     * @param battery Battery object.
     * @param display Java ImageView object.
     * @param textPercentage Java TextView object.
     */
    protected void displayBatteryLevel(Battery battery, ImageView display, TextView textPercentage) {
        String batteryLevelAprox = battery.getBatteryLevelAprox();
        String percentage = (int) battery.getPercentage() + "%";

        switch (batteryLevelAprox) {
            case Battery.BATTERY_EMPTY:
                display.setImageResource(R.drawable.battery_empty);
                break;
            case Battery.BATTERY_5:
                display.setImageResource(R.drawable.battery_5);
                break;
            case Battery.BATTERY_10:
                display.setImageResource(R.drawable.battery_10);
                break;
            case Battery.BATTERY_20:
                display.setImageResource(R.drawable.battery_20);
                break;
            case Battery.BATTERY_30:
                display.setImageResource(R.drawable.battery_30);
                break;
            case Battery.BATTERY_40:
                display.setImageResource(R.drawable.battery_40);
                break;
            case Battery.BATTERY_50:
                display.setImageResource(R.drawable.battery_50);
                break;
            case Battery.BATTERY_60:
                display.setImageResource(R.drawable.battery_60);
                break;
            case Battery.BATTERY_70:
                display.setImageResource(R.drawable.battery_70);
                break;
            case Battery.BATTERY_80:
                display.setImageResource(R.drawable.battery_80);
                break;
            case Battery.BATTERY_90:
                display.setImageResource(R.drawable.battery_90);
                break;
            case Battery.BATTERY_95:
                display.setImageResource(R.drawable.battery_95);
                break;
            case Battery.BATTERY_100:
                display.setImageResource(R.drawable.battery_100);
                break;
            case Battery.NO_BATTERY:
                display.setImageResource(R.drawable.no_battery);
                break;
            case Battery.BATTERY_ERROR:
                display.setImageResource(R.drawable.no_battery);
                break;
            default:
                break;
        }

        textPercentage.setText(percentage);
    }

}
