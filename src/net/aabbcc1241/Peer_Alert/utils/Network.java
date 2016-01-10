package net.aabbcc1241.Peer_Alert.utils;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Created by beenotung on 1/11/16.
 */
public class Network {
    private static final String TAG = "utils.Network";

    public static String getLocalIpWifi(Context context) {
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    /**
     * based on : http://stackoverflow.com/questions/6064510/how-to-get-ip-address-of-the-device
     */
    public static Vector<String> getLocalIpAddresses(boolean ipv4Only) {
        Vector<String> ips = new Vector<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
//                        String ip = Formatter.formatIpAddress(inetAddress.hashCode());
                        String ip = inetAddress.getHostAddress();
//                        Log.i(TAG, "***** IP=" + ip);
                        if (ipv4Only) {
                            if (ip.contains("."))
                                ips.add(ip);
                        } else
                            ips.add(ip);
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return ips;
    }
}
