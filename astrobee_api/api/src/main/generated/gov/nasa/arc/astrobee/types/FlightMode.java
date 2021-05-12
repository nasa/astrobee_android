// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum FlightMode {
    OFF("Off"),
    QUIET("Quiet"),
    NOMINAL("Nominal"),
    DIFFICULT("Difficult"),
    PRECISION("Precision");

    private final String m_value;

    FlightMode(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
