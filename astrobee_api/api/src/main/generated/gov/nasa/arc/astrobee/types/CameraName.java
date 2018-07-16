// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum CameraName {
    SCIENCE("Science"),
    NAVIGATION("Navigation"),
    HAZARD("Hazard"),
    DOCK("Dock"),
    PERCH("Perch");

    private final String m_value;

    CameraName(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
