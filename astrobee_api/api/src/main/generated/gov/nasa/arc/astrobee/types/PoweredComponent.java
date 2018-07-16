// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum PoweredComponent {
    LASER_POINTER("Laser Pointer"),
    PAYLOAD_TOP_AFT("Payload Top Aft"),
    PAYLOAD_BOTTOM_AFT("Payload Bottom Aft"),
    PAYLOAD_BOTTOM_FRONT("Payload Bottom Front"),
    PMC("PMC");

    private final String m_value;

    PoweredComponent(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
