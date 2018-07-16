package gov.nasa.arc.astrobee.android.gs.test.guest_science_apk;

import android.content.Intent;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

/**
 * Created by kmbrowne on 10/26/17.
 */

public class StartTestGuestScienceApkService extends StartGuestScienceService {
    private static StartTestGuestScienceApkService sSingleton;

    private String mLatestCommand;

    private int mImageWidth, mImageHeight;

    @Override
    public void onGuestScienceCustomCmd(String command) {
        mLatestCommand = command;
        sendReceivedCustomCommand("info");
        try {
            JSONObject obj = new JSONObject(command);
            String mCommandStr = obj.getString("name");
            Log.i("onGuestScienceCustomCmd", "Test apk received command " + mCommandStr + ".");
            if (mCommandStr.contentEquals("showImage")) {
                // TODO(Katie) Add some code to display a candid image
            } else if (mCommandStr.contentEquals("storeImage")) {
                // TODO(Katie) Add code to store candid image
            } else if (mCommandStr.contentEquals("resizeImage")) {
                mImageWidth = obj.getInt("width");
                mImageHeight = obj.getInt("height");
                Log.i("onGuestScienceCustomCmd", "Height changed to " +
                        Integer.toString(mImageWidth) + " and width changed to"
                        + " " + Integer.toString(mImageHeight) + ".");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getLatestCommand() {
        return mLatestCommand;
    }

    @Override
    public void onGuestScienceStart() {
        Intent startApkActivity = new Intent(this, MainActivity.class);
        startActivity(startApkActivity);

        sSingleton = this;
        mImageWidth = 512;
        mImageHeight = 512;

        String path = getGuestScienceDataBasePath();
        String json_path = "{\"GS Data Base Path\": \"" + path + "\"}";
        sendData(MessageType.JSON, "info", json_path);
    }

    @Override
    public void onGuestScienceStop() {
        sendStopped("info");
        if (MainActivity.getSingleton() != null) {
            MainActivity.getSingleton().onGuestScienceStop();
        }
        terminate();
    }

    public static StartTestGuestScienceApkService getInstance() {
        return sSingleton;
    }
}
