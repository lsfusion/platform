package lsfusion.http.provider.logics;

import com.google.common.net.InetAddresses;
import lsfusion.base.BaseUtils;
import org.apache.log4j.Logger;
import org.springframework.security.web.util.matcher.IpAddressMatcher;

import java.net.*;
import java.util.*;

// the hosts a request may choose an application server on besides the configured one: host names, compared as written,
// addresses, and subnets (address/bits); an address is compared in its canonical form, a name is not resolved
public class AllowedHosts {

    private final static Logger logger = Logger.getLogger(AllowedHosts.class);

    private final Set<String> hosts = new HashSet<>();
    private final List<IpAddressMatcher> subnets = new ArrayList<>();

    public AllowedHosts(String allowedHosts) { // comma-separated
        for (String allowedHost : allowedHosts.split(",")) {
            allowedHost = allowedHost.trim();
            if (allowedHost.contains("/"))
                subnets.add(getSubnet(allowedHost));
            else if (!allowedHost.isEmpty())
                hosts.add(getCanonical(allowedHost));
        }
    }

    private static IpAddressMatcher getSubnet(String subnet) {
        int slash = subnet.indexOf('/');
        String address = subnet.substring(0, slash);
        Integer bits = BaseUtils.parseInt(subnet.substring(slash + 1));
        if (!isAddress(address) || bits == null || bits < 0 || bits > InetAddresses.forString(address).getAddress().length * 8)
            throw new IllegalArgumentException("Wrong subnet in allowedHosts: " + subnet);
        return new IpAddressMatcher(getCanonical(address) + "/" + bits);
    }

    // the subnets of this computer's own private addresses, on the interfaces that are up
    public static AllowedHosts getLocalSubnets() {
        List<String> subnets = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            if (networkInterfaces != null)
                for (NetworkInterface networkInterface : Collections.list(networkInterfaces))
                    if (isUp(networkInterface))
                        for (InterfaceAddress interfaceAddress : networkInterface.getInterfaceAddresses()) {
                            String subnet = getLocalSubnet(interfaceAddress.getAddress(), interfaceAddress.getNetworkPrefixLength());
                            if (subnet != null)
                                subnets.add(subnet);
                        }
        } catch (SocketException e) {
            logger.error("The network interfaces are not known, no local subnet is allowed: " + e.getMessage());
        }
        return new AllowedHosts(String.join(",", subnets));
    }

    private static boolean isUp(NetworkInterface networkInterface) {
        try {
            return networkInterface.isUp();
        } catch (SocketException e) { // the interface is gone
            return false;
        }
    }

    // the subnet of an interface's address if the address is a private one, kept within its private range: a site-local
    // or IPv6 unique local address with its interface's prefix, a loopback address or one with a prefix not known (0 / -1) alone
    static String getLocalSubnet(InetAddress address, int prefix) {
        byte[] bytes = address.getAddress();
        int bits = bytes.length * 8;
        int rangePrefix;
        if (address.isLoopbackAddress())
            rangePrefix = bits;
        else if (address instanceof Inet4Address && address.isSiteLocalAddress())
            rangePrefix = bytes[0] == 10 ? 8 : (bytes[0] & 0xff) == 172 ? 12 : 16; // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        else if (address instanceof Inet6Address && (bytes[0] & 0xfe) == 0xfc)
            rangePrefix = 7; // fc00::/7
        else if (address instanceof Inet6Address && address.isSiteLocalAddress())
            rangePrefix = 10; // fec0::/10
        else
            return null;
        String host = InetAddresses.toAddrString(address).replaceFirst("%.*", ""); // without the interface's scope
        return host + "/" + (prefix <= 0 ? bits : Math.min(Math.max(prefix, rangePrefix), bits));
    }

    // an address in its canonical form, a name as it is
    public static String getCanonical(String host) {
        return isAddress(host) ? InetAddresses.toAddrString(InetAddresses.forString(host)) : host;
    }

    public boolean contains(String host) {
        host = getCanonical(host);
        if (!isAddress(host))
            return hosts.contains(host);
        if (InetAddresses.forString(host).isAnyLocalAddress()) // the address of no host
            return false;
        String address = host;
        return hosts.contains(address) || subnets.stream().anyMatch(subnet -> subnet.matches(address));
    }

    private static boolean isAddress(String host) { // an address literal: a name is not resolved
        return host.matches("[0-9A-Fa-f:.]+") && InetAddresses.isInetAddress(host);
    }
}
