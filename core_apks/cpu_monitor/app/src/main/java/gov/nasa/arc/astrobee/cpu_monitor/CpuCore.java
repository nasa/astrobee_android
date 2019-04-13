
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

import java.util.Arrays;

/**
 * Cpu core abstraction for managing CPU core data
 */
public class CpuCore {

    // References values for Java Objects
    private Cpu physicalCpu;
    private int coreNumber;

    // Values for ROS messages
    private boolean enabled;
    private float[] loads;
    private long frequency;
    private long maxFrequency;

    public CpuCore(int coreNumber) {
        this.physicalCpu = new Cpu(Cpu.HIGH_LEVEL_PROCESSOR_NAME);
        this.coreNumber = coreNumber;
    }

    /**
     * Update the values for ROS messages
     */
    public void updateCpuCoreData() {
        this.enabled = checkPresence();
        this.frequency = calculateKernelCoreFrequency();
        this.maxFrequency = calculateMaxFrequency();
        this.loads = calculateLoads();
    }

    /**
     * Read CPU files and determine if cpu core is present or not.
     *
     * @return Boolean value: present or not
     */
    private boolean checkPresence() {
        String freqResult = Utils.readLineFile(physicalCpu.getCpuFolderPath()
                + "cpu" + this.coreNumber + "/online");
        return freqResult.equals("1") ? true : false;
    }

    /**
     * Calculate loads for this core from stat file
     *
     * @return Float array or null if error
     */
    private float[] calculateLoads() {
        CpuLoad load = physicalCpu.getStatInfo().getCpus().get(coreNumber + 1);
        float[] loads = null;

        if(load == null) {
            return null;
        }

        try {
            loads = new float[] {
                    (float) load.getPercentageNice(),
                    (float) load.getPercentageUser(),
                    (float) load.getPercentageSystem(),
                    (float) load.getPercentageVirtual(),
                    (float) load.getPercentageTotal(),
            };
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return loads;
        }
    }

    /**
     * Get frequency from scaling_cur_freq file. This method is useful with nonroot access devices
     *
     * @return Frequency in Hz or 0 if error (since it means the core is not present)
     */
    private long calculateKernelCoreFrequency() {
        String freqResult = Utils.readLineFile(physicalCpu.getCpuFolderPath()
                + "cpu" + this.coreNumber + "/cpufreq/scaling_cur_freq");
        return freqResult == null ? 0 : (Long.parseLong(freqResult)*1000);
    }

    /**
     * Get max frequency from cpuinfo_max_freq.
     *
     * @return Frequency in Hz or 0 if error
     */
    private long calculateMaxFrequency() {
        String freqResult = Utils.readLineFile(physicalCpu.getCpuFolderPath()
                + "cpu" + this.coreNumber + "/cpufreq/cpuinfo_max_freq");
        return freqResult == null ? 0 : (Long.parseLong(freqResult)*1000);
    }

    /**
     * Get the CPU core information in a single String
     *
     * @return String data
     */
    public String toString() {
        String loads = "";
        for (float load : this.loads) {
            loads += load + "   ";
        }
        return "\n number: " + coreNumber
                + "\n enabled: " + enabled
                + "\n loads: " + loads
                + "\n freq: " + frequency
                + "\n maxfreq: " + maxFrequency;
    }

    // Getter and Setter section begins here

    public Cpu getPhysicalCpu() {
        return physicalCpu;
    }

    public void setPhysicalCpu(Cpu physicalCpu) {
        this.physicalCpu = physicalCpu;
    }

    public int getCoreNumber() {
        return coreNumber;
    }

    public void setCoreNumber(int coreNumber) {
        this.coreNumber = coreNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public float[] getLoads() {
        return loads;
    }

    public void setLoads(float[] loads) {
        this.loads = loads;
    }

    public long getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public long getMaxFrequency() {
        return maxFrequency;
    }

    public void setMaxFrequency(int maxFrequency) {
        this.maxFrequency = maxFrequency;
    }
}
