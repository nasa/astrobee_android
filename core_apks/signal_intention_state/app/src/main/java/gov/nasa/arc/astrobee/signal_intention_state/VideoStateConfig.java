package gov.nasa.arc.astrobee.signal_intention_state;

/**
 * Created by rgarciar on 5/18/18.
 */

public class VideoStateConfig {

    private String rosState;
    private String localVideoName;
    private boolean loop;
    private String nextDefaultState;
    private boolean appRunner;
    private boolean appStopper;

    public VideoStateConfig() {
        rosState = null;
        localVideoName = null;
        loop = false;
        nextDefaultState = null;
        appRunner = false;
        appStopper = false;
    }

    public VideoStateConfig(String rosState, String localVideoName, boolean loop, String nextDefaultState, boolean appRunner, boolean appStopper) {
        this.rosState = rosState;
        this.localVideoName = localVideoName;
        this.loop = loop;
        this.nextDefaultState = nextDefaultState;
        this.appRunner = appRunner;
        this.appStopper = appStopper;
    }

    public String getRosState() {
        return rosState;
    }

    public String getLocalVideoName() {
        return localVideoName;
    }

    public boolean isLoop() {
        return loop;
    }

    public String getNextDefaultState() {
        return nextDefaultState;
    }

    public boolean isAppRunner() {
        return appRunner;
    }

    public boolean isAppStopper() {
        return appStopper;
    }

    public void setRosState(String rosState) {
        this.rosState = rosState;
    }

    public void setLocalVideoName(String localVideoName) {
        this.localVideoName = localVideoName;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setNextDefaultState(String nextDefaultState) {
        this.nextDefaultState = nextDefaultState;
    }

    public void setAppRunner(boolean appRunner) {
        this.appRunner = appRunner;
    }

    public void setAppStopper(boolean appStopper) {
        this.appStopper = appStopper;
    }
}
