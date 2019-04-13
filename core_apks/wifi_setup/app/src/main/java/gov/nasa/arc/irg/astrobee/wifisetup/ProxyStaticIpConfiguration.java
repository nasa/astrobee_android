package gov.nasa.arc.irg.astrobee.wifisetup;

import android.net.LinkAddress;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public final class ProxyStaticIpConfiguration {
    private static Class s_staticConfigCls = null;
    private static Field s_ipAddressFld = null;
    private static Field s_gatewayFld = null;
    private static Field s_dnsServersFld = null;
    private static Field s_domainsFld = null;
    private static Constructor<LinkAddress> s_linkAddrCtor = null;

    public static void initialize() throws ReflectiveOperationException {
        if (s_linkAddrCtor != null)
            return;

        s_staticConfigCls = Class.forName("android.net.StaticIpConfiguration");
        s_ipAddressFld = s_staticConfigCls.getField("ipAddress");
        s_gatewayFld = s_staticConfigCls.getField("gateway");
        s_dnsServersFld = s_staticConfigCls.getField("dnsServers");
        s_domainsFld = s_staticConfigCls.getField("domains");

        s_linkAddrCtor = LinkAddress.class.getConstructor(InetAddress.class, int.class);
    }

    private final Object m_staticIpConfig;

    public ProxyStaticIpConfiguration() throws ReflectiveOperationException {
        initialize();
        m_staticIpConfig = s_staticConfigCls.newInstance();
    }

    public Object getStaticIpConfiguration() {
        return m_staticIpConfig;
    }

    public void setIpAddress(final LinkAddress addr) throws IllegalAccessException {
        s_ipAddressFld.set(m_staticIpConfig, addr);
    }

    public void setIpAddress(final InetAddress addr, int prefix)
            throws ReflectiveOperationException
    {
        setIpAddress(s_linkAddrCtor.newInstance(addr, prefix));
    }

    public void setGateway(final InetAddress addr) throws IllegalAccessException {
        s_gatewayFld.set(m_staticIpConfig, addr);
    }

    public void setDnsServers(final List<InetAddress> servers) throws IllegalAccessException {
        @SuppressWarnings("unchecked")
        final ArrayList<InetAddress> currServers =
                (ArrayList<InetAddress>) s_dnsServersFld.get(m_staticIpConfig);
        currServers.clear();
        currServers.addAll(servers);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProxyStaticIpConfiguration: \n");
        sb.append(m_staticIpConfig.toString());
        return sb.toString();
    }
}
