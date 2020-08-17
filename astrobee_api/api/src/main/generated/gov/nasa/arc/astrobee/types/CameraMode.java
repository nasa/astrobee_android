// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum CameraMode {
    BOTH("Both"),
    RECORDING("Recording"),
    STREAMING("Streaming");

    private final String m_value;

    CameraMode(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
