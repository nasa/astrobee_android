

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

package gov.nasa.arc.irg.astrobee.picoflexx;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SettingsReceiver extends BroadcastReceiver {
    private static final String TAG = "SettingsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: started");

        if (intent.hasExtra(Constants.EXTRA_MASTER_URI)) {
            final String masterUri = intent.getStringExtra(Constants.EXTRA_MASTER_URI);

            Settings s = Settings.getInstance();
            SharedPreferences.Editor e = s.getSharedPreferences().edit();
            e.putString(Settings.KEY_MASTER_URI, masterUri);
            e.apply();

            Log.d(TAG, "onReceive: set master uri to: " + masterUri);
        }
    }
}
