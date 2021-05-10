// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum TelemetryType {
    COMM_STATUS("CommStatus"),
    CPU_STATE("CpuState"),
    DISK_STATE("DiskState"),
    EKF_STATE("EkfState"),
    GNC_STATE("GncState"),
    PMC_CMD_STATE("PmcCmdState"),
    POSITION("Position"),
    SPARSE_MAPPING_POSE("SparseMappingPose");

    private final String m_value;

    TelemetryType(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
