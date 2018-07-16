// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum FlashlightLocation {
    BACK("Back"),
    FRONT("Front");

    private final String m_value;

    FlashlightLocation(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
