
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {

    View.OnClickListener listener = null;
    Button btnStart;
    Button btnStop;
    TextView tvDetails;
    TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Interface widgets
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStop = (Button) findViewById(R.id.btn_stop);
        tvDetails = (TextView) findViewById(R.id.txt_details);
        tvStatus = (TextView) findViewById(R.id.txt_publisher_status);

        // Listener for click events, not useful without an interface
        listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Context ctx = getApplicationContext();
                if(v.equals(btnStart)) {
                    Intent intent = new Intent(ctx, DiskManagerService.class);
                    startService(intent);
                    update("SERVICE STARTED", true);
                } else if(v.equals(btnStop)) {
                    Intent intent = new Intent(ctx, DiskManagerService.class);
                    stopService(intent);
                    update("SERVICE STOPPED", false);
                }
            }
        };

        btnStart.setOnClickListener(listener);
        btnStop.setOnClickListener(listener);

        // Launch service
        Intent intent = new Intent(this, DiskManagerService.class);
        startService(intent);
        update("SERVICE STARTED", true);


    }

    @Override
    protected void onDestroy() {
        Intent intent = new Intent(this, DiskManagerService.class);
        stopService(intent);
        super.onDestroy();
    }

    private void update(String textForDetails, boolean isRunning) {
        if(textForDetails != null) {
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            tvDetails.setText(tvDetails.getText() + "\n" + currentDateTimeString + " --> " + textForDetails);
        }
        if(isRunning) {
            tvStatus.setText("SERVICE RUNNING");
            tvStatus.setTextColor(Color.GREEN);
        } else {
            tvStatus.setText("SERVICE STOPPED");
            tvStatus.setTextColor(Color.YELLOW);
        }

    }
}
