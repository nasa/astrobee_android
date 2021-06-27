// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum FlightMode {
    OFF("off"),
    QUIET("quiet"),
    NOMINAL("nominal"),
    DIFFICULT("difficult"),
    PRECISION("precision");

    private final String m_value;

    FlightMode(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
