// Copyright 2017 Intelligent Robotics Group, NASA ARC

package gov.nasa.arc.astrobee.types;

public enum PlannerType {
    TRAPEZOIDAL("trapezoidal"),
    QP("qp");

    private final String m_value;

    PlannerType(final String value) {
        m_value = value;
    }

    @Override
    public String toString() {
        return m_value; 
    }
}
