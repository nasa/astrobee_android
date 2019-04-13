package gov.nasa.arc.irg.astrobee.api_test_example;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Created by andres on 3/8/18.
 */

public class StartTestGuestScienceApkService extends StartGuestScienceService{

    private static final String LOGTAG = "StartGS_APKService";
    private static StartTestGuestScienceApkService gsApkServiceSingleton;

    private String mLatestCommand;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        mLatestCommand = command;
        try {
            JSONObject obj = new JSONObject(command);
            String mCmdStr = obj.getString("name");
            Log.i("onGuesScienceCustomCmd", "APK received command: " + mCmdStr + ".");
            if (mCmdStr.contentEquals("moveTo")){
                MainActivity.getSingleton().switchMove();
            } else if (mCmdStr.contentEquals("armTest")) {
                MainActivity.getSingleton().switchArm();
            } else if (mCmdStr.contentEquals("dockTest")) {
                MainActivity.getSingleton().dockTest();
            } else if (mCmdStr.contentEquals("lightTest")) {
                MainActivity.getSingleton().lightTest();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onGuestScienceStart() {
        Intent startApkActivity = new Intent(this, MainActivity.class);
        startActivity(startApkActivity);
        gsApkServiceSingleton = this;
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        if(MainActivity.getSingleton() != null){
            MainActivity.getSingleton().onGuestScienceStop();
        }
        terminate();
    }

    public static StartTestGuestScienceApkService getInstance() {
        return gsApkServiceSingleton;
    }
}
