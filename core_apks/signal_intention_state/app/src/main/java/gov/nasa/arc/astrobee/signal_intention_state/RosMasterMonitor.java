package gov.nasa.arc.astrobee.signal_intention_state;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.ros.node.NodeConfiguration;

import java.lang.ref.WeakReference;
import java.net.InetAddress;

public class RosMasterMonitor {

    private Boolean isRosMasterRunning;
    private InetAddress rosMasterIp;
    private NodeConfiguration nodeConfiguration;
    private Handler handler;

    private Runnable pollMaster = new Runnable() {
        @Override
        public void run() {
            RosMasterMonitorTask task = new RosMasterMonitorTask(RosMasterMonitor.this);
            task.execute(rosMasterIp, nodeConfiguration, isRosMasterRunning);
            handler.postDelayed(pollMaster, 5000);
        }
    };

    private static class RosMasterMonitorTask extends AsyncTask<Object, Void, Boolean> {
        private WeakReference<RosMasterMonitor> monitorWeakReference;

        RosMasterMonitorTask(RosMasterMonitor context) {
            monitorWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Boolean doInBackground(Object... objects) {
            InetAddress rosMasterIp = (InetAddress) objects[0];
            NodeConfiguration nodeConfiguration = (NodeConfiguration) objects[1];
            Boolean isRosMasterRunning = (Boolean) objects[2];

            Boolean returnValue = null;

            if (rosMasterIp != null && nodeConfiguration != null) {
                Log.i("SIGNAL_STATE", "Polling ROS master");
                boolean rosMasterAvailable = Utils.isRosMasterAvailable(rosMasterIp.getHostAddress(),
                        nodeConfiguration.getMasterUri().getPort());

                if (!isRosMasterRunning && rosMasterAvailable) {
                    // FSW is running
                    returnValue = true;
                } else if (isRosMasterRunning && !rosMasterAvailable) {
                    // FSW stopped
                    returnValue = false;
                }
            }

            return returnValue;

        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean == null) {
                return;
            }

            RosMasterMonitor monitor = monitorWeakReference.get();
            if (monitor != null) {
                monitor.isRosMasterRunning = aBoolean;
            }
            EventBus.getDefault().post(aBoolean
                    ? new MessageEvent(MessageEvent.ROS_MASTER_SHOW_UP)
                    : new MessageEvent(MessageEvent.ROS_MASTER_WENT_AWAY));
        }
    }

    private static class IpResolutionTask extends AsyncTask<NodeConfiguration, Void, InetAddress> {

        private WeakReference<RosMasterMonitor> monitorWeakReference;

        IpResolutionTask(RosMasterMonitor context) {
            monitorWeakReference = new WeakReference<>(context);
        }

        @Override
        protected InetAddress doInBackground(NodeConfiguration... nodeConfigurations) {
            return Utils.resolveRosMasterIp(nodeConfigurations[0]);
        }

        @Override
        protected void onPostExecute(InetAddress inetAddress) {
            RosMasterMonitor monitor = monitorWeakReference.get();
            if (monitor != null) {
                monitor.rosMasterIp = inetAddress;
            }
        }
    }

    public RosMasterMonitor(NodeConfiguration nodeConfiguration) {
        this.nodeConfiguration = nodeConfiguration;
        this.isRosMasterRunning = false;
        this.handler = new Handler();
        new IpResolutionTask(this).execute(nodeConfiguration);
    }

    public void start() {
        handler.post(pollMaster);
    }

    public void stop() {
        handler.removeCallbacks(pollMaster);
    }
}
