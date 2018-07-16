// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum ActionType {
    PAN("Pan"),
    TILT("Tilt"),
    BOTH("Both");

    private final String m_value;

    ActionType(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
