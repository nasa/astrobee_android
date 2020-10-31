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

package gov.nasa.arc.irg.astrobee.sci_cam_image2;

import java.lang.Thread;
import android.util.Log;

// Take a picture if the camera is not in use, mark the camera
// in use, and wait until the picture is processed and the
// camera is again not marked in use before continuing.

public class PictureThread implements Runnable {

    private MainActivity m_parent;

    public PictureThread(MainActivity parent) {
        m_parent = parent;
    }
    
    public void run() {
            
        while (true) {
            try {

                // TODO: Not sure if all the logic here must be in the
                // critical section, but it is safer that way
                
                // Quit if told so
                if (m_parent.doQuit) {
                    break;
                }
                
                if (!m_parent.continuousPictureTaking && !m_parent.takeSinglePicture) {
                    Thread.sleep(250); // sleep (milliseconds)
                    continue;
                }
                
                if (m_parent.inUse) {
                    // Wait until the picture is processed
                    if (MainActivity.doLog)
                        Log.i(MainActivity.SCI_CAM_TAG, "Camera in use, will wait");
                    // For some reason, sleeping here too little
                    // results in camera jamming. Should not happen.
                    // Not sure why.
                    Thread.sleep(150); // sleep (milliseconds)
                    continue;
                }

                // Take the picture
                if (MainActivity.doLog)
                    Log.i(MainActivity.SCI_CAM_TAG, "Will take a picture");

                synchronized(m_parent) {
                    // Mark the camera in use
                    m_parent.inUse = true;

                    if (m_parent.cc != null) {
                        if (m_parent.doLog)
                            Log.i(MainActivity.SCI_CAM_TAG, "Trying to take a picture with preview");
                        m_parent.cc.takePicture();
                    }
                    
                    // If only one picture is needed, declare it taken
                    if (m_parent.takeSinglePicture)
                        m_parent.takeSinglePicture = false;
                }
                
            } catch (Exception e) {
                continue;
            }
                    
        }
            
    }
    
}
