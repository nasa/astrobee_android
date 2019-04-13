
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

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Java class for managing values from /proc/stat related with a single CPU
 */
public class CpuLoad {

    // Real values from /proc/stat
    private String name = "";
    private int cpu_user = 0;
    private int cpu_nice = 0;
    private int cpu_system = 0;
    private int cpu_idle = 0;
    private int cpu_iowait = 0;
    private int cpu_irq = 0;
    private int cpu_softirqs = 0;
    private int cpu_steal = 0;
    private int cpu_guest = 0;
    private int cpu_guest_nice = 0;

    // Calculated variables
    private int cal_user = 0;
    private int cal_nice = 0;
    private int cal_system = 0;
    private int cal_system_all = 0;
    private int cal_idle = 0;
    private int cal_idle_all = 0;
    private int cal_irq = 0;
    private int cal_softirqs = 0;
    private int cal_steal = 0;
    private int cal_guest_all = 0;
    private int cal_total = 0;

    // Calculated percentages
    private double percentage_nice;
    private double percentage_user;
    private double percentage_system;
    private double percentage_virtual;
    private double percentage_total;

    public CpuLoad() {

    }

    public CpuLoad(String name, int cpu_user, int cpu_niced, int cpu_system, int cpu_idle,
                   int cpu_iowait, int cpu_irq, int cpu_softirqs, int cpu_steal, int cpu_guest,
                   int cpu_guest_nice) {
        this.name = name;
        this.cpu_user = cpu_user;
        this.cpu_nice = cpu_niced;
        this.cpu_system = cpu_system;
        this.cpu_idle = cpu_idle;
        this.cpu_iowait = cpu_iowait;
        this.cpu_irq = cpu_irq;
        this.cpu_softirqs = cpu_softirqs;
        this.cpu_guest = cpu_guest;
        this.cpu_guest_nice = cpu_guest_nice;
    }

    // Getter and setters for real values from /proc/stat

    public String getName() {
        return name;
    }

    public int getCpuUser() {
        return cpu_user;
    }

    public int getCpuNice() {
        return cpu_nice;
    }

    public int getCpuSystem() {
        return cpu_system;
    }

    public int getCpuIdle() {
        return cpu_idle;
    }

    public int getCpuIowait() {
        return cpu_iowait;
    }

    public int getCpuIrq() {
        return cpu_irq;
    }

    public int getCpuSoftirqs() {
        return cpu_softirqs;
    }

    public int getCpuSteal() {
        return cpu_steal;
    }

    public int getCpuGuest() {
        return cpu_guest;
    }

    public int getCpuGuestNice() {
        return cpu_guest_nice;
    }

    
    // Calculated values following FFTeam rules

    public int getCalUser() {
        return cal_user;
    }

    public void setCalUser(int cal_user) {
        this.cal_user = cal_user;
    }

    public int getCalNice() {
        return cal_nice;
    }

    public void setCalNice(int cal_nice) {
        this.cal_nice = cal_nice;
    }

    public int getCalSystem() {
        return cal_system;
    }

    public void setCalSystem(int cal_system) {
        this.cal_system = cal_system;
    }

    public int getCalSystemAll() {
        return cal_system_all;
    }

    public void setCalSystemAll(int cal_system_all) {
        this.cal_system_all = cal_system_all;
    }

    public int getCalIdle() {
        return cal_idle;
    }

    public void setCalIdle(int cal_idle) {
        this.cal_idle = cal_idle;
    }

    public int getCalIdleAll() {
        return cal_idle_all;
    }

    public void setCalIdleAll(int cal_idle_all) {
        this.cal_idle_all = cal_idle_all;
    }

    public int getCalIrq() {
        return cal_irq;
    }

    public void setCalIrq(int cal_irq) {
        this.cal_irq = cal_irq;
    }

    public int getCalSoftirqs() {
        return cal_softirqs;
    }

    public void setCalSoftirqs(int cal_softirqs) {
        this.cal_softirqs = cal_softirqs;
    }

    public int getCalSteal() {
        return cal_steal;
    }

    public void setCalSteal(int cal_steal) {
        this.cal_steal = cal_steal;
    }

    public int getCalGuestAll() {
        return cal_guest_all;
    }

    public void setCalGuestAll(int cal_guest_all) {
        this.cal_guest_all = cal_guest_all;
    }

    public int getCalTotal() {
        return cal_total;
    }

    public void setCalTotal(int cal_total) {
        this.cal_total = cal_total;
    }


    // Percentages

    public double getPercentageNice() {
        return percentage_nice;
    }

    public void setPercentageNice(double percentage_nice) {
        this.percentage_nice = percentage_nice;
    }

    public double getPercentageUser() {
        return percentage_user;
    }

    public void setPercentageUser(double percentage_user) {
        this.percentage_user = percentage_user;
    }

    public double getPercentageSystem() {
        return percentage_system;
    }

    public void setPercentageSystem(double percentage_system) {
        this.percentage_system = percentage_system;
    }

    public double getPercentageVirtual() {
        return percentage_virtual;
    }

    public void setPercentageVirtual(double percentage_virtual) {
        this.percentage_virtual = percentage_virtual;
    }

    public double getPercentageTotal() {
        return percentage_total;
    }

    public void setPercentageTotal(double percentage_total) {
        this.percentage_total = percentage_total;
    }

    public double getAverageIdlePercentage() {
        return (cpu_idle * 100) / (cpu_user + cpu_nice + cpu_system + cpu_idle + cpu_iowait + cpu_irq + cpu_softirqs);
    }

    @Override
    public String toString() {
        return "CpuLoad{" +
                "name='" + name + '\'' +
                ", cpu_user=" + cpu_user +
                ", cpu_nice=" + cpu_nice +
                ", cpu_system=" + cpu_system +
                ", cpu_idle=" + cpu_idle +
                ", cpu_iowait=" + cpu_iowait +
                ", cpu_irq=" + cpu_irq +
                ", cpu_softirqs=" + cpu_softirqs +
                ", cpu_steal=" + cpu_steal +
                ", cpu_guest=" + cpu_guest +
                ", cpu_guest_nice=" + cpu_guest_nice +
                ", cal_user=" + cal_user +
                ", cal_nice=" + cal_nice +
                ", cal_system=" + cal_system +
                ", cal_system_all=" + cal_system_all +
                ", cal_idle=" + cal_idle +
                ", cal_idle_all=" + cal_idle_all +
                ", cal_irq=" + cal_irq +
                ", cal_softirqs=" + cal_softirqs +
                ", cal_steal=" + cal_steal +
                ", cal_guest_all=" + cal_guest_all +
                ", cal_total=" + cal_total +
                ", percentage_nice=" + percentage_nice +
                ", percentage_user=" + percentage_user +
                ", percentage_system=" + percentage_system +
                ", percentage_virtual=" + percentage_virtual +
                ", percentage_total=" + percentage_total +
                '}';
    }
}
