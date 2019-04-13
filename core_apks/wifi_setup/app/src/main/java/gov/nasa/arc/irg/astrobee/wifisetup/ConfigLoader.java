package gov.nasa.arc.irg.astrobee.wifisetup;

import android.net.wifi.WifiConfiguration;
import android.util.Log;

import org.json.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ConfigLoader {
    private static final String TAG = "SetupService";

    public static List<WifiConfiguration> loadConfigs(final File f) throws IOException {
        if (!f.exists()) {
            throw new FileNotFoundException("No such file");
        }

        CharBuffer cb = CharBuffer.allocate((int) f.length());

        InputStreamReader isr = new InputStreamReader(
                new BufferedInputStream(
                        new FileInputStream(f)), StandardCharsets.UTF_8);

        isr.read(cb);

        isr.close();

        if (!f.delete())
            Log.w(TAG, "Unable to delete file");

        cb.rewind();
        return loadConfigs(cb.toString());
    }

    public static List<WifiConfiguration> loadConfigs(final String s) {
        final List<WifiConfiguration> list = new LinkedList<>();
        final JSONTokener t = new JSONTokener(s);

        try {
            while (t.more()) {
                Object v = t.nextValue();

                if (v instanceof JSONObject) {
                    list.add(loadConfig((JSONObject) v));
                } else if (v instanceof JSONArray) {
                    final JSONArray a = (JSONArray) v;
                    for (int i = 0; i < a.length(); i++) {
                        final JSONObject o = a.getJSONObject(i);
                        list.add(loadConfig(o));
                    }
                }
            }
        } catch (JSONException e) {
            // Ignore "End of Input" exceptions
            if (t.more())
                throw new RuntimeException("Malformed JSON given", e);
        }

        return list;
    }

    private static final String CCMP = "CCMP";
    private static final String TKIP = "TKIP";

    private static final String WPA = "WPA";
    private static final String RSN = "RSN";
    private static final String WPA2 = "WPA2";

    private static final String DHCP = "DHCP";

    public static WifiConfiguration loadConfig(final JSONObject o) {
        final WifiConfiguration config = new WifiConfiguration();

        try {
            config.SSID = o.getString("ssid");
            config.hiddenSSID = o.optBoolean("hidden", false);
            config.preSharedKey = o.optString("psk", null);

            Object netObj = o.opt("network");
            if (netObj == null ||
                    (netObj instanceof String && ((String) netObj).equalsIgnoreCase(DHCP))) {
                ProxyIpConfiguration ipConfig = new ProxyIpConfiguration();
                ipConfig.setIpAssignment(ProxyIpConfiguration.IpAssignment.DHCP);
                ProxyWifiConfiguration proxyConfig = new ProxyWifiConfiguration(config);
                proxyConfig.setIpConfiguration(ipConfig);
            } else if (netObj instanceof JSONObject) {
                ProxyIpConfiguration ipConfig = parseNetwork((JSONObject) netObj);
                ProxyWifiConfiguration proxyConfig = new ProxyWifiConfiguration(config);
                proxyConfig.setIpConfiguration(ipConfig);
            } else {
                throw new RuntimeException("Invalid network config specified");
            }

            Object protocolObj = o.opt("protocol");
            if (protocolObj instanceof String) {
                parseProtocols(config.allowedProtocols,
                        Collections.singletonList((String) protocolObj));
            } else if (protocolObj instanceof JSONArray) {
                final JSONArray a = (JSONArray) protocolObj;
                final ArrayList<String> strings = new ArrayList<>(a.length());
                for (int i = 0; i < a.length(); i++) {
                    strings.add(a.getString(i));
                }
                parseProtocols(config.allowedProtocols, strings);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Invalid WiFi config", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Unable to setup a proper config", e);
        }

        return config;
    }

    private static void parseProtocols(final BitSet protocols, final List<String> strings) {
        final String[] protocolStrings = WifiConfiguration.Protocol.strings;
        for (final String s : strings) {
            for (int i = 0; i < protocolStrings.length; i++) {
                if (protocolStrings[i].equalsIgnoreCase(s)) {
                    protocols.set(i, true);
                }
            }
        }
    }

    private static ProxyIpConfiguration parseNetwork(final JSONObject o)
            throws JSONException, ReflectiveOperationException
    {
        final ProxyStaticIpConfiguration sip = new ProxyStaticIpConfiguration();
        try {
            int prefix = o.getInt("prefix");
            if (prefix < 0 || prefix > 32)
                throw new RuntimeException("Invalid prefix");

            sip.setIpAddress(InetAddress.getByName(o.getString("ip")), o.getInt("prefix"));
            sip.setGateway(InetAddress.getByName(o.optString("gateway", "0.0.0.0")));

            final Object dns = o.opt("dns");
            if (dns instanceof String) {
                sip.setDnsServers(Arrays.asList(InetAddress.getByName((String) dns)));
            } else if (dns instanceof JSONArray) {
                final JSONArray servers = (JSONArray) dns;
                ArrayList<InetAddress> addrs = new ArrayList<>(servers.length());
                for (int i = 0; i < servers.length(); i++) {
                    addrs.add(InetAddress.getByName(servers.getString(i)));
                }
                sip.setDnsServers(addrs);
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException("Invalid ip, gateway or dns server", e);
        }

        final ProxyIpConfiguration config = new ProxyIpConfiguration();
        config.setIpAssignment(ProxyIpConfiguration.IpAssignment.STATIC);
        config.setStaticIpConfiguration(sip);
        return config;
    }
}
