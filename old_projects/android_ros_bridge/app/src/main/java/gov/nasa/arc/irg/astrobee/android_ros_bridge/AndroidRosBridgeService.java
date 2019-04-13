package gov.nasa.arc.irg.astrobee.android_ros_bridge;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import ff_msgs.CommandConstants;

/**
 * Created by amora on 5/16/17.
 */

public class AndroidRosBridgeService extends Service{

    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_CMD_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    static final int MSG_GS_TO_ROBOT = 5;
    static final int MSG_SERVICE_TO_ROBOT = 6;
    static final int MSG_SERVICE_TO_GS = 7;
    static final int MSG_ROBOT_TO_SERVICE = 8;

    NotificationManager mNM;
    private static ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    final Messenger mMessenger = new Messenger(new IncomingMessageHandler());
    private static final String LOGTAG = "ARBService";

    private GuestScienceRosMessages gsRosMsgs = GuestScienceRosMessages.getSingletonInstance();

    public void onCreate() {
        super.onCreate();
        Log.i(LOGTAG, "Service Started!");
        showNotification();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGTAG, "Received start id: " + startId + ": " + intent + " flags: " + flags);
        Toast.makeText(this, "Service Started!", Toast.LENGTH_LONG).show();
        return START_STICKY; // Run until explicitly stopped.
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mNM.cancel(R.string .local_service_label);
        Toast.makeText(this, "Service Destroyed!", Toast.LENGTH_LONG).show();
        Log.i(LOGTAG, "Service Destroyed!");
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(LOGTAG, "onBind");
        return mMessenger.getBinder();
    }

    private void showNotification() {
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        CharSequence text = getText(R.string.service_started);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the icon, scrolling text and timestamp
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.local_service_label))
                .setContentText(text)
                .setContentIntent(contentIntent)
                .build();

        mNM.notify(R.string.local_service_label, notification);
    }

    /**
     * Handle incoming messages from Clients
     */
    private class IncomingMessageHandler extends android.os.Handler {
        @Override
        public void handleMessage(Message msg) {
            String msgRxd;
            Log.d(LOGTAG,"handleMessage: " + msg.what);
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_SET_CMD_VALUE:
                    msgRxd = msg.getData().getString("cmd");
                    Log.i(LOGTAG, msgRxd);
                    break;
                case MSG_GS_TO_ROBOT:
                    msgRxd = msg.getData().getString("cmd");
                    Log.i(LOGTAG, msgRxd);
                    translateGSToRobot(msg.getData());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void translateGSToRobot(Bundle rxdBundle) {

        switch (rxdBundle.getString("cmd")){
            case CommandConstants.CMD_NAME_STOP_ALL_MOTION:
                sendMessageToClient(MSG_SERVICE_TO_ROBOT, CommandConstants.CMD_NAME_STOP_ALL_MOTION +"1");
                gsRosMsgs.stopAllMotion();
                break;

            case CommandConstants.CMD_NAME_ARM_PAN_AND_TILT:
                sendMessageToClient(MSG_SERVICE_TO_ROBOT, CommandConstants.CMD_NAME_ARM_PAN_AND_TILT +"1");
                gsRosMsgs.armPanAndTilt(rxdBundle.getFloat("angle1"), rxdBundle.getFloat("angle2"), rxdBundle.getString("cmpToMove"));
                break;


            case ff_msgs.CommandConstants.CMD_NAME_SIMPLE_MOVE6DOF:
                sendMessageToClient(MSG_SERVICE_TO_ROBOT, CommandConstants.CMD_NAME_SIMPLE_MOVE6DOF +"1");
                gsRosMsgs.move(CommandConstants.CMD_NAME_SIMPLE_MOVE6DOF, rxdBundle.getDoubleArray("pos"));
                break;
        }
    }

    /**
     * Method forwards String response from robot(simulator) to client
     * Right now the message received by the service(AR Bridge) and displayed on the Guest Science app
     * and the one intended for the Guest Science app only are the same.
     * If further treatment or an special translation from the "raw" message sent by the robot is
     * required, they should be different messages.
     * @param strToSend
     */
    public static void setMsgRobotToService(String strToSend) {
        sendMessageToClient(MSG_ROBOT_TO_SERVICE, strToSend);
        sendMessageToClient(MSG_SERVICE_TO_GS, strToSend);
    }


    /**
     * Send the data to all clients.
     * @param strToSend The String message to send.
     */
    private static void sendMessageToClient(int typeOfMsg, String strToSend) {
        Iterator<Messenger> messengerIterator = mClients.iterator();
        while(messengerIterator.hasNext()) {
            Messenger messenger = messengerIterator.next();
            try {
                switch (typeOfMsg) {
                    case MSG_SERVICE_TO_ROBOT:
                        messenger.send(messageComposer(messenger, MSG_SERVICE_TO_ROBOT, strToSend));
                        break;
                    case MSG_SERVICE_TO_GS:
                        messenger.send(messageComposer(messenger, MSG_SERVICE_TO_GS, strToSend));
                        break;
                    case MSG_ROBOT_TO_SERVICE:
                        messenger.send(messageComposer(messenger, MSG_ROBOT_TO_SERVICE, strToSend));
                        break;
                }

            } catch (RemoteException e) {
                // The client is dead. Remove it from the list.
                mClients.remove(messenger);
            }
        }
    }

    /**
     * @param messenger What client to send msg to
     * @param typeOfMsg What type of msg is the service sending to client
     * @param strToSend What String to send to client
     */
    private static Message messageComposer(Messenger messenger, int typeOfMsg, String strToSend) {
        // Send data as a String
        Bundle bundle = new Bundle();
        bundle.putString("robotResponse", strToSend);
        Message msg = Message.obtain(null, typeOfMsg);
        msg.setData(bundle);
        return msg;
    }
}
