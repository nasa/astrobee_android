// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum LocalizationMode {
    NONE("None"),
    MAPPED_LANDMARKS("MappedLandmarks"),
    ARTAGS("ARTags"),
    HANDRAIL("Handrail"),
    PERCH("Perch"),
    TRUTH("Truth");

    private final String m_value;

    LocalizationMode(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
