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

package gov.nasa.arc.irg.astrobee.sci_cam_image;

import android.media.Image;
import android.util.Log;
import android.util.Size;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.Date;

public class ImageHandler implements Runnable {

    private boolean m_saveImage;

    // The JPEG image
    private final Image m_image;

    private SciCamPublisher m_sciCamPublisher;

    private String m_dataPath;

    public ImageHandler(Image image, boolean save, String dataPath) {
        m_dataPath = dataPath;
        m_image = image;
        m_saveImage = save;
        m_sciCamPublisher = SciCamPublisher.getInstance();
    }

    // Create the output file name. We use a name representing
    // seconds (with fractional part) since epoch, in the same way
    // ROS timestamps are represented.
    public File getOutputDataFile(long secs, long nsecs) {
        File dataStorageDir = new File(m_dataPath);

        // Create the storage directory if it does not exist
        if (!dataStorageDir.exists()) {
            if (!dataStorageDir.mkdirs()) {
                return null;
            }
        }

        String timestamp = String.format("%d.%d", secs, nsecs);
        return new File(dataStorageDir + File.separator + timestamp + ".jpg");
    }

    public void run() {
        Log.d(StartSciCamImage.TAG, "run: Handling captured image!");

        if (m_image == null) {
            Log.e(StartSciCamImage.TAG, "run: Captured image is null!");
            return;
        }

        // Record time
        // TODO (Katie) Use for testing
        Date date = new Date();
        long current_time = date.getTime();
        long secs = current_time/1000;
        long nsecs = (current_time - secs * 1000) * 1000;

        long image_time_nanosecs = m_image.getTimestamp();
        long image_secs = image_time_nanosecs/1000000000;
        long image_nsecs = (image_time_nanosecs - (secs * 1000000000));

        long diff_secs = secs - image_secs;
        long diff_nsecs = nsecs - image_nsecs;

        Log.d(StartSciCamImage.TAG, "Current time: " + current_time);
        Log.d(StartSciCamImage.TAG, "Image time: " + image_time_nanosecs);
        Log.d(StartSciCamImage.TAG, "diff secs: " + diff_secs + " diff_nsecs: " + diff_nsecs);

        ByteBuffer buffer = m_image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Size imageSize = new Size(m_image.getWidth(), m_image.getHeight());

        m_sciCamPublisher.publishImage(bytes, imageSize, secs, nsecs);

        if (m_saveImage) {
            // Image file
            File imageFile = getOutputDataFile(secs, nsecs);
            FileOutputStream outputStream = null;
            try {
                Log.d(StartSciCamImage.TAG, "Writing image to file: " + imageFile);
                outputStream = new FileOutputStream(imageFile);
                outputStream.write(bytes);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(StartSciCamImage.TAG, "run: Error saving image!", e);
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(StartSciCamImage.TAG, " run: Error closing output stream!", e);
                    }
                }
            }
        }

        // This is very important, otherwise the app will crash next time around
        Log.d(StartSciCamImage.TAG, "run: Closing image!");
        m_image.close();
    }
}
