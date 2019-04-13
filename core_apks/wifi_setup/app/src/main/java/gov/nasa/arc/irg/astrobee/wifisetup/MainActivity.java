package gov.nasa.arc.irg.astrobee.wifisetup;

import android.app.Activity;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private WifiManager m_wifiManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        try {
            ProxyIpConfiguration pic = new ProxyIpConfiguration();
            pic.setIpAssignment(ProxyIpConfiguration.IpAssignment.STATIC);

            ProxyStaticIpConfiguration psip = new ProxyStaticIpConfiguration();
            psip.setIpAddress(InetAddress.getByName("192.168.1.1"), 24);
            psip.setDnsServers(Arrays.asList(InetAddress.getByName("8.8.8.8")));

            pic.setStaticIpConfiguration(psip);
            Log.i(TAG, pic.toString());
        } catch (Exception e) {
            Log.e(TAG, "Exception doing Proxy things", e);
        }

        m_wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        List<WifiConfiguration> networks = m_wifiManager.getConfiguredNetworks();
        if (networks == null)
            Log.i(TAG, "Returned null for list of configurations");

        if (networks == null || networks.size() == 0)
            Log.i(TAG, "No networks configured");
        else {
            WifiInfo info = m_wifiManager.getConnectionInfo();
            if (info != null && info.getNetworkId() != -1) {
                if (m_wifiManager.disconnect())
                    Log.i(TAG, "Disconnected from network " + info.getNetworkId());
                else {
                    Log.e(TAG, "Unable to disconnect from current network");
                }
            }

            for (WifiConfiguration c : networks) {
                Log.i(TAG, "Network: " + c.SSID + ", id: " + c.networkId);
                if (!m_wifiManager.removeNetwork(c.networkId)) {
                    Log.e(TAG, "Unable to remove network");
                    if (!m_wifiManager.disableNetwork(c.networkId)) {
                        Log.e(TAG, "Unable to disable network");
                    }
                }
            }
            m_wifiManager.saveConfiguration();
        }
        */
    }
}
