package gov.nasa.arc.astrobee.android.gs.manager;

import android.os.CountDownTimer;

/**
 * Created by kmbrowne on 12/13/17.
 */

public class ManagerTimeoutTimer extends CountDownTimer {
    public ManagerTimeoutTimer(long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
    }

    @Override
    public void onTick(long millisUntilFinished) {
        // Do nothing since we are waiting for a timeout
        ManagerNode.INSTANCE().getLogger().debug("ManagerTimeoutTimer",
                "Still waiting for the apk to start or stop. " + Float.toString(millisUntilFinished)
                + " milliseconds left.");
    }

    @Override
    public void onFinish() {
        String apkName = ManagerNode.INSTANCE().getCurrentApkName();
        String errMsg = "Apk " + apkName + " didn't start or stop in the timeout. Please see the" +
                " guest science library documentation for more information.";
        ManagerNode.INSTANCE().ackGuestScienceStart(false, apkName, errMsg);
    }
}
