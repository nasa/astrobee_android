
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

import android.content.Intent;
import android.util.ArrayMap;
import android.util.Log;

import org.apache.commons.lang.StringUtils;

import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Static Java class with general useful methods related to CPU management.
 */
public class CpuUtils {

    private static final String TAG = CpuUtils.class.getSimpleName();

    /**
     * Normal processes executing in user mode
     */
    private static final int CPU_USER=1;
    /**
     * Niced processes executing in user mode
     */
    private static final int CPU_NICE=2;
    /**
     * Processes executing in kernel mode
     */
    private static final int CPU_SYSTEM=3;
    /**
     * Twiddling thumbs
     */
    private static final int CPU_IDLE=4;
    /**
     * Waiting for I/O to complete
     */
    private static final int CPU_IOWAIT=5;
    /**
     * Servicing interrupts
     */
    private static final int CPU_IRQ=6;
    /**
     * Servicing softirqs
     */
    private static final int CPU_SOFTIRQS=7;
    /**
     * Ticks spent executing other virtual hosts
     */
    private static final int CPU_STEAL=8;
    /**
     * Time spent running a virtual CPU for guest operating systems under the control of
     * the Linux kernel
     */
    private static final int CPU_GUEST=9;
    /**
     * Guest nice
     */
    private static final int CPU_GUEST_NICE=10;

    /**
     * Read the stat file from the path assigned to a Cpu given and parse it in a Stat java object
     *
     * @param cpu Cpu to analyze
     * @return StatFile with new data or empty cpus if error
     */
    public static StatFile parseStatsFile(Cpu cpu) {
        StatFile statFile = new StatFile();
        CpuLoad[] array = new CpuLoad[cpu.getCores().size() + 1];
        //Log.i("LOG", "Cores" + array.length);
        try {
            RandomAccessFile reader = new RandomAccessFile(cpu.getStatPath(), "r");
            try {
                while (true) {
                    String load = reader.readLine();
                    //Log.d(TAG, "Stat: " + load);
                    //Avoid problem parsing doble space
                    if(load!=null) {
                        //Log.d(TAG, "Stat: " + load);
                        load = load.replace("  ", " ");
                        String[] tokens = load.split(" ");
                        if (tokens[0].startsWith("cpu")) {
                            String number = StringUtils.substring(tokens[0], 3, 4);
                            int coreNumber;
                            CpuLoad cpuLoad = parseCpuTokens(tokens);

                            if(StringUtils.trim(number).equals("")) {
                                array[0] = cpuLoad;
                            } else {
                                coreNumber = Integer.parseInt(number);
                                // TODO check if corenumber is a digit
                                array[coreNumber + 1] = cpuLoad;
                            }
                        }
                    } else {
                        break;
                    }
                }

                ArrayList<CpuLoad> arrayList = new ArrayList<>(Arrays.asList(array));

                statFile.setCpus(arrayList);
            } catch (EOFException ex) {
                ex.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
            throw new IllegalStateException("Unable to access the stats");
        }

        return statFile;
    }

    /**
     * Take an array of String and assign them to a CpuLoad java object
     *
     * @param tokens String with load values from stat file
     * @return CpuLoad java object with data from stat file
     */
    private static CpuLoad parseCpuTokens(String[] tokens){
        CpuLoad cpu = new CpuLoad(tokens[0],
                Integer.parseInt(tokens[CPU_USER]),
                Integer.parseInt(tokens[CPU_NICE]),
                Integer.parseInt(tokens[CPU_SYSTEM]),
                Integer.parseInt(tokens[CPU_IDLE]),
                Integer.parseInt(tokens[CPU_IOWAIT]),
                Integer.parseInt(tokens[CPU_IRQ]),
                Integer.parseInt(tokens[CPU_SOFTIRQS]),
                Integer.parseInt(tokens[CPU_STEAL]),
                Integer.parseInt(tokens[CPU_GUEST]),
                Integer.parseInt(tokens[CPU_GUEST_NICE]));
        return cpu;
    }

    /**
     * Read the CPU folder path and get the number of cores with files in there
     *
     * @param cpuFolderPath
     * @return Number of cores or 1 per default.
     */
    public static int calculatePhysicalNumberCores(String cpuFolderPath) {

        // Private Class to display only CPU devices in the directory listing
        class CpuFilter implements FileFilter {

            @Override
            public boolean accept(File pathname) {

                // Check if filename is "cpu", followed by a single digit number
                if (Pattern.matches("cpu[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {

            // Get directory containing CPU info
            File dir = new File(cpuFolderPath);

            // Filter to only list the devices we care about
            File[] files = dir.listFiles(new CpuFilter());

            // Return the number of cores (virtual CPU devices)
            return files.length;
        } catch (Exception e) {

            // Default to return 1 core
            return 1;
        }
    }

    /**
     * Read the thermal folder looking for thermal_zones.
     *
     * @param tempFolderPath
     * @return Number of thermal zones inside folder given.
     */
    public static int calculateNumberOfThermalZones(String tempFolderPath) {

        // Private Class to display only thermal zones in the directory listing
        class ThermalFilter implements FileFilter {

            @Override
            public boolean accept(File pathname) {

                //Check if filename is "thermal_zone", followed by digit numbers
                if (Pattern.matches("thermal_zone[0-9]+", pathname.getName())) {
                    return true;
                }
                return false;
            }
        }

        try {

            //Get directory containing thermal info
            File dir = new File(tempFolderPath);

            //Filter to only list the thermal zones
            File[] files = dir.listFiles(new ThermalFilter());

            //Return the number of thermal zones
            return files.length;
        } catch (Exception e) {
            return 0;
        }
    }
}
