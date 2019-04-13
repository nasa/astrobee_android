package gov.nasa.arc.irg.astrobee.wifisetup;

import android.app.IntentService;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class SetupService extends IntentService {
    private static final String TAG = "SetupService";

    public static final String ACTION_CLEAR = "gov.nasa.arc.irg.astrobee.wifisetup.CLEAR";
    public static final String ACTION_LOAD  = "gov.nasa.arc.irg.astrobee.wifisetup.LOAD";
    public static final String ACTION_LIST  = "gov.nasa.arc.irg.astrobee.wifisetup.LIST";
    public static final String ACTION_DELETE  = "gov.nasa.arc.irg.astrobee.wifisetup.DELETE";
    public static final String EXTRA_PATH   = "gov.nasa.arc.irg.astrobee.wifisetup.EXTRA_PATH";
    public static final String EXTRA_NETID   = "gov.nasa.arc.irg.astrobee.wifisetup.EXTRA_NETID";

    private WifiManager m_wifiManager;

    public SetupService() {
        super("SetupService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        m_wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
    }

    private boolean checkWifi() {
        if (m_wifiManager == null) {
            Log.wtf(TAG, "WifiManager is missing. Something is wrong");
            return false;
        }

        if (!m_wifiManager.isWifiEnabled()) {
            Log.e(TAG, "Wifi not enabled. Enable wifi first.");
            return false;
        }

        return true;
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_CLEAR.equalsIgnoreCase(action)) {
                handleActionClear();
            } else if (ACTION_LIST.equalsIgnoreCase(action)) {
                handleActionList();
            } else if (ACTION_LOAD.equalsIgnoreCase(action)) {
                final String path = intent.getStringExtra(EXTRA_PATH);
                if (path == null) {
                    Log.e(TAG, "No path given");
                    return;
                }
                handleActionLoad(path);
            } else if (ACTION_DELETE.equalsIgnoreCase(action)) {
                final int netId = intent.getIntExtra(EXTRA_NETID, -1);
                if (netId == -1) {
                    Log.e(TAG, "No id specified");
                    return;
                }
                handleActionDelete(netId);
            } else {
                if (action.startsWith(getPackageName())) {
                    Log.wtf(TAG, "Unknown command: " + action.substring(action.lastIndexOf('.') + 1));
                    return;
                }
                Log.wtf(TAG, "Unknown action: " + action);
            }
        }
    }

    /**
     * Clear all existing wifi networks (for those we can actually clear)
     */
    private void handleActionClear() {
        if (!checkWifi()) return;

        boolean save = false;

        List<WifiConfiguration> networks = m_wifiManager.getConfiguredNetworks();
        for (WifiConfiguration net : networks) {
            if (m_wifiManager.removeNetwork(net.networkId)) {
                save = true;
            } else {
                Log.w(TAG, "Unable to remove network: " + net.SSID + ": Permission denied.");
            }
        }

        if (save)
            m_wifiManager.saveConfiguration();

        Log.i(TAG, "Cleared networks as permitted.");
    }

    private void handleActionDelete(final int netId) {
        if (!checkWifi()) return;

        if (m_wifiManager.removeNetwork(netId)) {
            m_wifiManager.saveConfiguration();
            Log.i(TAG, "Removed network with id " + netId);
        } else {
            Log.e(TAG, "Unable to remove network with id " + netId);
        }
    }

    private void handleActionLoad(final String path) {
        if (!checkWifi()) return;

        int added = 0;
        int numNets = 0;

        try {
            List<WifiConfiguration> configs = ConfigLoader.loadConfigs(new File(path));
            numNets = configs.size();

            for (final WifiConfiguration c : configs) {
                final int id = m_wifiManager.addNetwork(c);
                if (id == -1) {
                    Log.w(TAG, "Failed to add network: " + c.SSID);
                    continue;
                }

                if (!m_wifiManager.enableNetwork(id, false)) {
                    Log.w(TAG, "Failed to enable added network: " + c.SSID);
                }
                added++;
            }
        } catch (IOException | RuntimeException e) {
            Log.e(TAG, "Unable to load configuration", e);
        }

        if (added > 0)
            m_wifiManager.saveConfiguration();

        if (numNets == 1) {
            if (added > 0) {
                Log.i(TAG, "Network added");
            } else {
                Log.e(TAG, "Failed to add network");
            }
        } else if (numNets > 1) {
            Log.i(TAG, "Added " + added + " of " + numNets + " provided networks");
        }
    }

    /**
     * List saved wifi networks
     */
    private void handleActionList() {
        if (!checkWifi()) return;

        List<WifiConfiguration> networks = m_wifiManager.getConfiguredNetworks();
        for (final WifiConfiguration net : networks) {
            Log.i(TAG, net.networkId + ": " + net.SSID);
        }
    }
}
