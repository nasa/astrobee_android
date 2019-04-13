
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class DiskUtils {
    public static final long ERROR = -1;
    public static final int BUILT_IN_INTERNAL_MEMORY = 1;
    public static final int ADOPTED_INTERNAL_MEMORY = 2;
    public static final int EXTERNAL_MEMORY = 3;

    public static final int GET_FREE_MEM = 1;
    public static final int GET_USED_MEM = 2;
    public static final int GET_TOTAL_MEM = 3;

    public static final String BUILT_IN_INTERNAL_PATH = "/mnt/sdcard";
    public static final String ADOPTED_INTERNAL_MEMORY_PATH = "/mnt/expand";
    public static final String EXTERNAL_MEMORY_PATH = "/mnt/media_rw";
    public static final String EXTERNAL_MEMORY_PATH_PUBLIC = "/storage";
    public static final String MOUNT_FILE = "/proc/mounts";


    public static long getMemorySizeFromStat(StatFs stat, int targetSpace) {
        if(stat == null) {
            return ERROR;
        }
        switch (targetSpace) {
            case GET_FREE_MEM:
                return stat.getFreeBytes();
            case GET_USED_MEM:
                return stat.getTotalBytes() - stat.getFreeBytes();
            case GET_TOTAL_MEM:
                return stat.getTotalBytes();
            default:
                return ERROR;
        }
    }

    public static ArrayList<String> getMountedPaths(int memoryType) {
        String pathToLookFor = null;
        ArrayList<String> paths = new ArrayList<>();

        switch (memoryType) {
            case EXTERNAL_MEMORY:
                pathToLookFor = EXTERNAL_MEMORY_PATH;
                break;
            case ADOPTED_INTERNAL_MEMORY:
                pathToLookFor = ADOPTED_INTERNAL_MEMORY_PATH;
                break;
            default:
                break;
        }

        if(pathToLookFor == null) {
            return null;
        }

        RandomAccessFile reader = null;

        try {
            reader = new RandomAccessFile(MOUNT_FILE, "r");
            while (true) {
                String line = reader.readLine();
                if(line == null) {
                    break;
                }
                if(line.contains(pathToLookFor)) {
                    int begin = line.indexOf(pathToLookFor);
                    int end = line.indexOf(" ", begin);
                    String path = line.substring(begin, end);
                    if(memoryType == EXTERNAL_MEMORY) {
                        path = path.replace(EXTERNAL_MEMORY_PATH, EXTERNAL_MEMORY_PATH_PUBLIC);
                    }
                    paths.add(path);
                }
            }
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paths;
    }
}
