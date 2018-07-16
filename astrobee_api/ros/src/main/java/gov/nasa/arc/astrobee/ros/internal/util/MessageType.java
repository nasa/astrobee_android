package gov.nasa.arc.astrobee.ros.internal.util;
/**
 * Created by kmbrowne on 11/21/17.
 *
 * MessageType - Used to specify the type of data being sent in an Android message.
 */

public enum MessageType {
    MESSENGER(0),
    STOP(1),
    CMD(2),
    JSON(3),
    STRING(4),
    BINARY(5);

    private final int mValue;

    MessageType(final int value) {
        mValue = value;
    }

    public int toInt() {
        return mValue;
    }

    public byte toByte() { return (byte) mValue; }
}

