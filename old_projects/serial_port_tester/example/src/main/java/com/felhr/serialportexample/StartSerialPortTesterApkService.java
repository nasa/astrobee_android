package com.felhr.serialportexample;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Created by andres on 3/30/18.
 */

public class StartSerialPortTesterApkService extends StartGuestScienceService {

    private static final String LOGTAG = "StartSPT_APKService";
    private static StartSerialPortTesterApkService apkServiceSingleton;

    private String mLatestCommand;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        mLatestCommand = command;
        try {
            JSONObject obj = new JSONObject(command);
            String mCmdStr = obj.getString("name");
            Log.i("onGuesScienceCustomCmd", "APK received command: " + mCmdStr + ".");
            if (mCmdStr.contentEquals("Send Data")){
                MainActivity.getSingleton().sendData("Hello World\n");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onGuestScienceStart() {
        Intent startApkActivity = new Intent(this, MainActivity.class);
        startActivity(startApkActivity);
        apkServiceSingleton = this;
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        if(MainActivity.getSingleton() != null){
            MainActivity.getSingleton().onGuestScienceStop();
        }
        terminate();
    }

    public static StartSerialPortTesterApkService getInstance() {
        return apkServiceSingleton;
    }
}

