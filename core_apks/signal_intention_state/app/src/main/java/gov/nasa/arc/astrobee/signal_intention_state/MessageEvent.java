package gov.nasa.arc.astrobee.signal_intention_state;

public class MessageEvent {

    public static final int ROS_MASTER_WENT_AWAY = 0;
    public static final int ROS_MASTER_SHOW_UP = 1;

    private final int message;

    public MessageEvent(int message) {
        this.message = message;
    }

    public int getMessage() {
        return message;
    }
}
