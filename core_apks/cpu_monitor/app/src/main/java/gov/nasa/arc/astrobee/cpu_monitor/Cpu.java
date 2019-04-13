
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

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ff_msgs.CpuStateStamped;
import std_msgs.Header;

/**
 * Cpu abstraction for managing CPU data
 */
public class Cpu {

    // File and folder paths for CPU information
    public static final String DEFAULT_CPU_STAT_FILE_PATH = "/proc/stat";
    public static final String DEFAULT_CPU_TEMP_FOLDER_PATH = "/sys/class/thermal/";
    public static final String DEFAULT_CPU_TEMP_FILE_PATH = "/sys/class/thermal/thermal_zone0/temp";
    public static final String DEFAULT_CPU_FOLDER_PATH = "/sys/devices/system/cpu/";

    public static final String HIGH_LEVEL_PROCESSOR_NAME = "hlp";

    // Fields to publish on ROS messages
    public static final String[] DEFAULT_LOAD_FIELDS = {
            CpuStateStamped.NICE,
            CpuStateStamped.USER,
            CpuStateStamped.SYS,
            CpuStateStamped.VIRT,
            CpuStateStamped.TOTAL
    };

    // File and folder paths for CPU information in this device
    private String statPath;
    private String tempFolderPath;
    private String tempFilePath;
    private String cpuFolderPath;

    // Values to be published on ROS messages
    private Header header;
    private String name;
    private float temperature;
    private List<String> loadFields;
    private float[] aveLoads;
    private ArrayList<CpuCore> cores;
    private StatFile statInfo = null;

    // Constructor
    public Cpu(String name) {
        this.statPath = DEFAULT_CPU_STAT_FILE_PATH;
        this.tempFolderPath = DEFAULT_CPU_TEMP_FOLDER_PATH;
        this.tempFilePath = DEFAULT_CPU_TEMP_FILE_PATH;
        this.cpuFolderPath = DEFAULT_CPU_FOLDER_PATH;
        this.loadFields = Arrays.asList(DEFAULT_LOAD_FIELDS);
        this.name = name;
        this.cores = new ArrayList<CpuCore>();
    }

    /**
     * Get the CPU information in a single String
     *
     * @return String data
     */
    public String toString() {
        String loads = "";
        for(float load : aveLoads) {
            loads += load + "   ";
        }
        String r = "\n name: " + name
                + "\n temp: " + temperature
                + "\n aveLoads: " + loads
                + "\n cores:\n";
        for(CpuCore core : cores) {
            r += "\n core number: " + core.getCoreNumber()
                    + core.toString() + "\n\n";
        }

        return r;
    }

    /**
     * Add a core to the core list on this CPU
     * @param core
     */
    public void addCore(CpuCore core) {
        core.setPhysicalCpu(this);
        this.cores.add(core);
    }

    /**
     * Update the fields on this object getting data from physical device
     *
     */
    public void updateCpuData() {
        // Read the stat file and get data
        this.statInfo = getCpuStatFileInfo();

        // Update each one of the cores inside core list
        for (CpuCore core : cores) {
            core.updateCpuCoreData();
        }

        // Read thermal zone and get temperature
        this.temperature = calculateTemperatureThermalZero();

        // Read stat object and calculate average loads
        this.aveLoads = calculateAveLoads();
    }

    /**
     * Read the CPU folder path and get the number of cores with files in there
     * @return Number of cores based on number of cpu folders.
     */
    public int calculatePhysicalNumberCores() {
        return CpuUtils.calculatePhysicalNumberCores(this.cpuFolderPath);
    }

    /**
     * Read the Stat file and calculate the values for each field to be shown on the ROS messages.
     * @return Stat File abstraction to Java Object
     */
    private StatFile getCpuStatFileInfo() {

        /*
         * This always returns a new StateFile with the current values from stat file without
         * the calculated values.
         */
        StatFile newS = CpuUtils.parseStatsFile(this);

        /*
         * Return the old state file values. It may be contain calculated values or being null.
         */
        StatFile oldF = this.statInfo;

        // Number of CPUs according to stat file. Including the actual physical CPU
        int statCpuLoadsNumber = newS.getCpus().size();


        // The final stat to return
        StatFile finalF = new StatFile();

        // TODO Check number of cores in stat against folder number.

        /*    Calculate values    */
        for (int i = 0; i < statCpuLoadsNumber; i++) {

            // Get the data for the cpu
            CpuLoad loadN = newS.getCpus().get(i);

            if (loadN == null) {
                loadN = new CpuLoad();
                loadN.setPercentageNice(0);
                loadN.setPercentageUser(0);
                loadN.setPercentageSystem(0);
                loadN.setPercentageVirtual(0);
                loadN.setPercentageTotal(0);
            } else {

                // User load is equal to user time minus guest time
                loadN.setCalUser(loadN.getCpuUser() - loadN.getCpuGuest());

                // Nice load is equal to nice time minus
                loadN.setCalNice(loadN.getCpuNice() - loadN.getCpuGuestNice());

                // System all load is equal to system + irq + softirqs time.
                loadN.setCalSystemAll(loadN.getCpuSystem() + loadN.getCpuIrq() + loadN.getCpuSoftirqs());

                // Idle all load equal to idle + iowait time
                loadN.setCalIdleAll(loadN.getCpuIdle() + loadN.getCpuIowait());

                // No changes
                loadN.setCalSystem(loadN.getCpuSystem());
                loadN.setCalIdle(loadN.getCpuIdle());
                loadN.setCalIrq(loadN.getCpuIrq());
                loadN.setCalSoftirqs(loadN.getCpuSoftirqs());
                loadN.setCalSteal(loadN.getCpuSteal());

                // Guest all is equal to guest + guest nice time
                loadN.setCalGuestAll(loadN.getCpuGuest() + loadN.getCpuGuestNice());

                // Total load
                loadN.setCalTotal(loadN.getCpuUser() + loadN.getCpuNice() + loadN.getCalSystemAll()
                        + loadN.getCalIdleAll() + loadN.getCalSteal() + loadN.getCalGuestAll());

                // Variables used to get loads on a period of time
                int userPeriod = loadN.getCalUser();
                int nicePeriod = loadN.getCalNice();
                int stealPeriod = loadN.getCalSteal();
                int guestPeriod = loadN.getCalGuestAll();
                int systemAllPeriod = loadN.getCalSystemAll();
                int totalPeriod = loadN.getCalTotal();

                // Prevent Null Pointer Exception
                if (oldF != null) {
                    CpuLoad loadO = oldF.getCpus().get(i);
                    userPeriod -= loadO.getCalUser();
                    nicePeriod -= loadO.getCalNice();
                    stealPeriod -= loadO.getCalSteal();
                    guestPeriod -= loadO.getCalGuestAll();
                    systemAllPeriod -= loadO.getCalSystemAll();
                    totalPeriod -= loadO.getCalTotal();
                }

                // Ensure absolute values
                userPeriod = Math.abs(userPeriod);
                nicePeriod = Math.abs(nicePeriod);
                stealPeriod = Math.abs(stealPeriod);
                guestPeriod = Math.abs(guestPeriod);
                systemAllPeriod = Math.abs(systemAllPeriod);
                totalPeriod = Math.abs(totalPeriod);

                // Avoid zero division
                if (totalPeriod == 0) {
                    totalPeriod = 1;
                }

                // Getting percentages
                double totalP = (double) totalPeriod;
                loadN.setPercentageNice(nicePeriod / totalP * 100);
                loadN.setPercentageUser(userPeriod / totalP * 100);
                loadN.setPercentageSystem(systemAllPeriod / totalP * 100);
                loadN.setPercentageVirtual((guestPeriod + stealPeriod) / totalP * 100);
                loadN.setPercentageTotal(loadN.getPercentageNice()
                        + loadN.getPercentageUser()
                        + loadN.getPercentageSystem()
                        + loadN.getPercentageVirtual());

            }

            // Adding data to Cpu List in final Stat File
            finalF.getCpus().add(loadN);
        }

        return finalF;
    }

    /**
     * Read all the thermal_zones and get an average with all the values different from zero.
     * This method don't be used to calculate CPU temperature. It should be deleted on the future.
     *
     * @return Average temperature based on thermal zones
     */
    private float calculateTemperature() {

        int numThermalZones = CpuUtils.calculateNumberOfThermalZones(this.tempFolderPath);
        int thermalZonesWithValue = 0;
        int tempAggregate = 0;

        for(int i = 0; i < numThermalZones; i++) {
            String result = Utils.readLineFile(tempFolderPath + "thermal_zone" + i + "/temp");

            int add = 0;
            if(result != null) {
                add = Integer.parseInt(result);
            }
            if(add != 0) thermalZonesWithValue++;
            tempAggregate += add;
        }
        float aveTemperature = (tempAggregate / thermalZonesWithValue) * 0.001f;

        return aveTemperature;
    }

    /**
     * Read the first thermal zone for the CPU temperature.
     * @return CPU temperature in Celsius degrees.
     */
    private float calculateTemperatureThermalZero() {
        String result = Utils.readLineFile(tempFilePath);
        float tempMillis = result == null ? -1000f : Float.parseFloat(result);
        return  tempMillis * 0.001f;
    }

    /**
     * Get the average percentage for all the CPU cores
     * @return Float array with based 100 values of null if error
     */
    private float[] calculateAveLoads() {
        // TODO Check with Katie if this is what they want
        CpuLoad load = statInfo.getCpus().get(0);
        float[] loads = null;
        try {
            loads = new float[] {
                    (float) load.getPercentageNice(),
                    (float) load.getPercentageUser(),
                    (float) load.getPercentageSystem(),
                    (float) load.getPercentageVirtual(),
                    (float) load.getPercentageTotal()
            };
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return loads;
        }
    }

    // Getter and Setter section

    public String getStatPath() {
        return statPath;
    }

    public void setStatPath(String statPath) {
        this.statPath = statPath;
    }

    public String getTempFolderPath() {
        return tempFolderPath;
    }

    public void setTempFolderPath(String tempFolderPath) {
        this.tempFolderPath = tempFolderPath;
    }

    public String getTempFilePath() {
        return tempFilePath;
    }

    public void setTempFilePath(String tempFilePath) {
        this.tempFilePath = tempFilePath;
    }

    public String getCpuFolderPath() {
        return cpuFolderPath;
    }

    public void setCpuFolderPath(String cpuFolderPath) {
        this.cpuFolderPath = cpuFolderPath;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public List<String> getLoadFields() {
        return loadFields;
    }

    public void setLoadFields(List<String> loadFields) {
        this.loadFields = loadFields;
    }

    public float[] getAveLoads() {
        return aveLoads;
    }

    public void setAveLoads(float[] aveLoads) {
        this.aveLoads = aveLoads;
    }

    public ArrayList<CpuCore> getCores() {
        return cores;
    }

    public void setCores(ArrayList<CpuCore> cores) {
        this.cores = cores;
    }

    public StatFile getStatInfo() {
        return statInfo;
    }

    public void setStatInfo(StatFile statInfo) {
        this.statInfo = statInfo;
    }
}
