package gov.nasa.arc.irg.astrobee.api_test_example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.FlashlightLocation;

public class MainActivity extends Activity implements View.OnClickListener {

    private static MainActivity mActivitySingleton;
    private StartTestGuestScienceApkService mGuestScienceServiceClass;

    private static final String LOGTAG = "MainActivity";

    private Button mBtn_startApp, mBtn_stopApp, mBtn_MoveTest, mBtn_ArmTest, mBtn_DockTest, mBtn_LightTest;
    private static ScrollView mSV_msgSentToRobot, mSV_msgRecvdFromRobot;
    private static TextView mTxt_msgSentToRobot, mTxt_msgRecvdFromRobot;

    private boolean b_move = false;
    private boolean b_arm = false;

    ApiCommandImplementation apiTest = null;

    public MainActivity () {
        //super("API_TEST_APP", "Example ROS Node", ROS_MASTER_URI);
    }

    public static MainActivity getSingleton() { return mActivitySingleton; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtn_startApp = (Button) findViewById(R.id.btn_startApp);
        mBtn_stopApp = (Button) findViewById(R.id.btn_stopApp);
        mBtn_MoveTest = (Button) findViewById(R.id.btn_moveTest);
        mBtn_ArmTest = (Button) findViewById(R.id.btn_armTest);
        mBtn_DockTest = (Button) findViewById(R.id.btn_dockTest);
        mBtn_LightTest = (Button) findViewById(R.id.btn_lighTest);

        mTxt_msgSentToRobot = (TextView) findViewById(R.id.text_msgSentToRobot);
        mTxt_msgRecvdFromRobot = (TextView) findViewById(R.id.text_msgRecvdFromRobot);

        mTxt_msgSentToRobot.setMovementMethod(new ScrollingMovementMethod());
        mTxt_msgRecvdFromRobot.setMovementMethod(new ScrollingMovementMethod());

        mSV_msgSentToRobot = (ScrollView) findViewById(R.id.scrollView_msgSentToRobot);
        mSV_msgRecvdFromRobot = (ScrollView) findViewById(R.id.scrollView_msgRecvdFromRobot);

        mBtn_startApp.setOnClickListener(this);
        mBtn_stopApp.setOnClickListener(this);
        mBtn_MoveTest.setOnClickListener(this);
        mBtn_ArmTest.setOnClickListener(this);
        mBtn_DockTest.setOnClickListener(this);
        mBtn_LightTest.setOnClickListener(this);

        apiTest = ApiCommandImplementation.getSingletonInstance();

        Log.i(LOGTAG, "Activity Started Successfully");
    }

    @Override
    public void onClick(View v) {
        if (v.equals(mBtn_startApp)) {
            //doStartService();
        } else if (v.equals(mBtn_stopApp)){
            doStopService();
        } else if (v.equals(mBtn_MoveTest)) {

        } else if (v.equals(mBtn_ArmTest)) {

        } else if (v.equals(mBtn_DockTest)) {
            dockTest();
        } else if (v.equals(mBtn_LightTest)) {
            lightTest();
        }
    }

    /*private void doStartService() {
    }*/

    private void doStopService() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }

    public void switchMove(){
        if(b_move) {
            b_move = false;
            moveToTest(0.0, 0.0, 0.0);
        } else {
            b_move = true;
            moveToTest(0.5, 0.5, 0.5);
        }
    }

    public void switchArm(){
        if(b_arm){
            b_arm = false;
            armTest(0.0f, 0.0f);
        } else {
            b_arm = true;
            armTest(10.0f, 5.0f);
        }
    }

    private void moveToTest(double x, double y, double z) {
        //double GOAL_X = 0.5, GOAL_Y = 0.0, GOAL_Z = 0.0;
        apiTest.moveTo(x, y, z);
        setmTxt_msgSentToRobot("Sent a Move Test command with x: " + x + ", y: " + y + ", z: " + z + "\n");
    }

    private void armTest(float angle1, float angle2) {
        //float angle1 = 10.0f, angle2 = 5.0f;
        ActionType pan = ActionType.PAN;
        setmTxt_msgSentToRobot("Sent a Arm Test command with angle1: " + angle1 + ", angle2:" + angle2 + "\n");
        apiTest.armTest(angle1, angle2, pan);
    }

    public void dockTest() {
        int var1 = 1;
        setmTxt_msgSentToRobot("Sent a Dock Test command. \n");
        apiTest.dockTest(var1);
    }

    public void lightTest() {
        FlashlightLocation var1 = FlashlightLocation.FRONT;
        float var2 = 0.0f;
        setmTxt_msgSentToRobot("Sent a Light Test command with Location: " + String.valueOf(var1) + ", value: " + var2 + "\n");
        apiTest.lightTest(var1, var2);
    }

    // This helps keeping the scroll to the last line within the ScrollView
    private static void scrollToBottom(final ScrollView sv) {
        sv.post(new Runnable() {
            @Override
            public void run() {
                sv.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    public static void setmTxt_msgSentToRobot(String msgTxd) {
        mTxt_msgSentToRobot.append(msgTxd + "\n");
        scrollToBottom(mSV_msgSentToRobot);
    }

    public static void setmTxt_msgRcvdToRobot(String msgRxd) {
        mTxt_msgRecvdFromRobot.append(msgRxd + "\n");
        scrollToBottom(mSV_msgRecvdFromRobot);
    }

    protected void onStart() {
        super.onStart();
        mGuestScienceServiceClass = StartTestGuestScienceApkService.getInstance();
        if (mGuestScienceServiceClass != null){
            //do something with the last command received
        }
    }

    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onGuestScienceStop() { this.finish(); }
}
