package gov.nasa.arc.astrobee.android.gs.test.guest_science_apk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import gov.nasa.arc.astrobee.android.gs.MessageType;

public class MainActivity extends Activity {
    private static MainActivity sSingleton;
    private TextView mTextView;
    private EditText mEditDataText;
    private StartTestGuestScienceApkService mServiceClass;

    public void displayCommand(String command) {
        mTextView.setText(command);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.tv_text);
        mEditDataText = (EditText) findViewById(R.id.et_data_box);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("OnStart", "Yays!!!!!");
        mServiceClass = StartTestGuestScienceApkService.getInstance();
        if (mServiceClass != null) {
            String command = mServiceClass.getLatestCommand();
            displayCommand(command);
        }
    }

    public void onClickSendStart(View v) {
       Log.i("onClickSendStart", "Send start message button clicked.");
        mServiceClass.sendStarted("info");
    }

    public void onClickSendStop(View v) {
        Log.i("onClickSendStop", "Send stop message button clicked.");
        mServiceClass.sendStopped("info");
    }

    public void onClickSendJson(View v) {
        Log.i("onClickSendJson", "Send JSON data button clicked.");
        String data = mEditDataText.getText().toString();
        mServiceClass.sendData(MessageType.JSON, "data", data);
    }

    public void onClickSendString(View v) {
        Log.i("onClickSendString", "Send string data button clicked.");
        String data = mEditDataText.getText().toString();
        mServiceClass.sendData(MessageType.STRING, "data", data);
    }

    public void onClickSendBinaryData(View v) {
        Log.i("onClickSendBinaryData", "Send binary data button clicked.");
        byte[] data = new byte[5];
        data[0] = 0x01;
        data[1] = 0x0A;
        data[2] = 0x10;
        data[3] = 0x1B;
        data[4] = 0x21;
        mServiceClass.sendData(MessageType.BINARY, "data", data);
    }

    public void onGuestScienceStop() {
        this.finish();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public static MainActivity getSingleton() {
        return sSingleton;
    }
}

