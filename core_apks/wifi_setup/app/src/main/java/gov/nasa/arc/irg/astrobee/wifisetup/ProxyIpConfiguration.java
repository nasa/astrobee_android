package gov.nasa.arc.irg.astrobee.wifisetup;

import java.lang.reflect.Field;
import java.util.EnumMap;

public final class ProxyIpConfiguration {
    public enum IpAssignment {
        STATIC, DHCP, UNASSIGNED;
    }

    private static Class s_ipConfigCls = null;
    private static Field s_ipAssignFld = null;
    private static Field s_staticConfigFld = null;
    private static final EnumMap<IpAssignment, Object> s_ipAssignmentMap =
            new EnumMap<>(IpAssignment.class);

    public static void initialize() throws ReflectiveOperationException {
        if (s_staticConfigFld != null)
            return;

        s_ipConfigCls = Class.forName("android.net.IpConfiguration");
        s_ipAssignFld = s_ipConfigCls.getDeclaredField("ipAssignment");
        s_staticConfigFld = s_ipConfigCls.getDeclaredField("staticIpConfiguration");

        @SuppressWarnings("unchecked")
        final Class<Enum> enumCls = (Class<Enum>) s_ipAssignFld.getType();
        for (final IpAssignment k : IpAssignment.values()) {
            s_ipAssignmentMap.put(k, Enum.valueOf(enumCls, k.name()));
        }
    }

    private final Object m_ipConfig;

    public ProxyIpConfiguration() throws ReflectiveOperationException {
        initialize();
        m_ipConfig = s_ipConfigCls.newInstance();
    }

    public IpAssignment getIpAssignment() throws IllegalAccessException {
        final Object v = s_ipAssignFld.get(m_ipConfig);
        for (EnumMap.Entry<IpAssignment, Object> e : s_ipAssignmentMap.entrySet()) {
           if (e.getValue() == v)
               return e.getKey();
        }

        throw new RuntimeException("This should never happen");
    }

    public void setIpAssignment(IpAssignment assignment) throws IllegalAccessException {
        s_ipAssignFld.set(m_ipConfig, s_ipAssignmentMap.get(assignment));
    }

    public void setStaticIpConfiguration(Object config) throws IllegalAccessException {
        s_staticConfigFld.set(m_ipConfig, config);
    }

    public void setStaticIpConfiguration(ProxyStaticIpConfiguration config)
            throws IllegalAccessException
    {
        setStaticIpConfiguration(config.getStaticIpConfiguration());
    }

    public Object getIpConfiguration() {
        return m_ipConfig;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ProxyIpConfiguration: \n");
        sb.append(m_ipConfig.toString());
        return sb.toString();
    }
}
