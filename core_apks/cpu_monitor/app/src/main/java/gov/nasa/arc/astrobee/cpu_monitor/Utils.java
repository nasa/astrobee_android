
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

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Environment;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Static java class with general tools
 */
public class Utils {

    /**
     * Read the first line on a file given and return its value
     * @param path File path to read
     * @return String for the first line or null if error
     *
     */
    public static String readLineFile(String path) {
        String load = null;
        RandomAccessFile reader = null;

        try {
            reader = new RandomAccessFile(path, "r");
            load = reader.readLine();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            return load;
        }
    }

    /**
     * Launch a simple toast message
     *
     * @param context
     * @param msg
     */
    public static void toastMessage(Context context, String msg) {

        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Launch a simple touch message to the main UI using activity. Useful on background tasks.
     *
     * @param act
     * @param msg
     */
    public static void toastMessageToMainUI(final Activity act, final String msg) {

        act.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(act, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}
