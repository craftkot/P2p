package dev.opentogether.fabric;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;

/** Suggests a LAN-only IPv4 address; it never claims Internet reachability. */
final class LanAddress {
    private LanAddress() {}

    static String suggest(int port) {
        if (port < 1 || port > 65535) return "";
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces == null) return "";
            for (NetworkInterface iface : Collections.list(interfaces)) {
                if (!iface.isUp() || iface.isLoopback() || iface.isVirtual()) continue;
                for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                    if (address instanceof Inet4Address ipv4 && ipv4.isSiteLocalAddress()
                            && !ipv4.isLoopbackAddress()) {
                        return ipv4.getHostAddress() + ":" + port;
                    }
                }
            }
        } catch (SocketException | SecurityException ignored) {
            // The host may use a privacy-restricted sandbox or no LAN interface.
        }
        return "";
    }
}
