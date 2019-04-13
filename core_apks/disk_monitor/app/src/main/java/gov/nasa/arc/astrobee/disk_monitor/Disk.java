
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

import android.os.StatFs;

public class Disk {
    private String label;
    private int diskType;
    private String path;
    private long capacity;
    private long used;


    public Disk(String label, int diskType, String path) {
        this.label = label;
        this.diskType = diskType;
        this.path = path;
    }

    public boolean updateData() {
        try {
            StatFs statFs = new StatFs(this.path);
            this.capacity = DiskUtils.getMemorySizeFromStat(statFs, DiskUtils.GET_TOTAL_MEM);
            this.used = DiskUtils.getMemorySizeFromStat(statFs, DiskUtils.GET_USED_MEM);
            return true;
        } catch (IllegalArgumentException e) {
            // Path doesn't exist
            e.printStackTrace();
            return false;
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getDiskType() {
        return diskType;
    }

    public void setDiskType(int diskType) {
        this.diskType = diskType;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getCapacity() {
        return capacity;
    }

    public void setCapacity(long capacity) {
        this.capacity = capacity;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }
}
