package gov.nasa.arc.astrobee.signal_intention_state;

import org.ros.node.NodeConfiguration;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import ff_msgs.SignalState;

public class Utils {
    public static String getStateNameFromId(byte value) {
        // Signal name
        String state = null;

        // Get all fields in SignalState class
        Field[] interfaceFields = SignalState.class.getFields();
        for (Field f : interfaceFields) {
            // Only check byte fields
            if (f.getType() == byte.class) {
                try {
                    // Check if the value we received is found in any field in the class.
                    if ((byte) f.get(null) == value) {
                        // Get the string name of the value
                        state = f.getName();
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return state;
    }

    public static Byte getStateIdFromName(String name) {
        Byte id = null;

        // Get all fields in SignalState class
        Field[] interfaceFields = SignalState.class.getFields();
        for (Field f : interfaceFields) {
            // Only check byte fields
            if (f.getType() == byte.class) {
                try {
                    // Check if the value we received is found in any field in the class.
                    if (f.getName().equals(name)) {
                        // Get the string name of the value
                        id = (Byte) f.get(null);
                        break;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        return id;

    }

    public static boolean isRosMasterAvailable(String host, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.close();
            return true;
        } catch (IOException e) {
            // Connection problem
            return false;
        }
    }

    public static InetAddress resolveRosMasterIp(NodeConfiguration nodeConfiguration) {
        InetAddress rosMasterIp = null;
        try {
            rosMasterIp = InetAddress.getByName(nodeConfiguration.getMasterUri().getHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return rosMasterIp;
    }
}
