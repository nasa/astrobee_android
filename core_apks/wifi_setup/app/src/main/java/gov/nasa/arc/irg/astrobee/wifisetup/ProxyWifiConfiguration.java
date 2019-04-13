package gov.nasa.arc.irg.astrobee.wifisetup;

import android.net.wifi.WifiConfiguration;

import java.lang.reflect.Field;

public class ProxyWifiConfiguration {

    private static Field s_ipConfigFld;

    public static void initialize() throws ReflectiveOperationException {
        if (s_ipConfigFld != null)
            return;

        s_ipConfigFld = WifiConfiguration.class.getDeclaredField("mIpConfiguration");
        s_ipConfigFld.setAccessible(true);
    }

    private final WifiConfiguration m_config;

    public ProxyWifiConfiguration(WifiConfiguration config) throws ReflectiveOperationException {
        initialize();
        m_config = config;
    }

    public void setIpConfiguration(final Object o) throws IllegalAccessException {
        s_ipConfigFld.set(m_config, o);
    }

    public void setIpConfiguration(ProxyIpConfiguration proxy) throws IllegalAccessException {
        setIpConfiguration(proxy.getIpConfiguration());
    }
}
