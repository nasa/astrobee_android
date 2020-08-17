// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum CameraResolution {
    R224X171("224x171"),
    R320X240("320x240"),
    R480X270("480x270"),
    R640X480("640x480"),
    R960X540("960x540"),
    R1024X768("1024x768"),
    R1280X720("1280x720"),
    R1280X960("1280x960"),
    R1920X1080("1920x1080");

    private final String m_value;

    CameraResolution(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
