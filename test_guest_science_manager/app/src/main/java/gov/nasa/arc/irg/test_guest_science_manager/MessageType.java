package gov.nasa.arc.irg.test_guest_science_manager;

/**
 * Created by kmbrowne on 11/10/17.
 */

public enum MessageType {
    MESSENGER(0),
    STOP(1),
    CMD(2),
    JSON(3),
    STRING(4),
    BINARY(5);

    private final int m_value;

    MessageType(final int value) {
        m_value = value;
    }

    public int toInt() {
        return m_value;
    }
}
