// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum DownloadMethod {
    IMMEDIATE("Immediate"),
    DELAYED("Delayed");

    private final String m_value;

    DownloadMethod(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
