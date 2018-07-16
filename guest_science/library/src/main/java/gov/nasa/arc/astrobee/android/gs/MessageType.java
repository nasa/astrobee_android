package gov.nasa.arc.astrobee.android.gs;

/**
 * Created by kmbrowne on 11/21/17.
 *
 * MessageType - Used to specify the type of data being sent in an Android message.
 */

public enum MessageType {
    MESSENGER(0),
    PATH(1),
    STOP(2),
    CMD(3),
    JSON(4),
    STRING(5),
    BINARY(6);

    private final int mValue;

    MessageType(final int value) {
        mValue = value;
    }

    public int toInt() {
        return mValue;
    }
}
