package lsfusion.http.provider.logics;

import org.junit.Test;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.Assert.*;

public class AllowedHostsTest {

    @Test
    public void containsTheHostsAsWritten() {
        AllowedHosts allowedHosts = new AllowedHosts(" app-2 , 10.0.0.7, 0:0:0:0:0:0:0:5,,0.0.0.0");
        assertTrue(allowedHosts.contains("app-2"));
        assertTrue(allowedHosts.contains("10.0.0.7"));
        assertTrue(allowedHosts.contains("::ffff:10.0.0.7")); // the same address
        assertTrue(allowedHosts.contains("::5"));
        assertFalse(allowedHosts.contains("APP-2"));
        assertFalse(allowedHosts.contains("app-2.other"));
        assertFalse(allowedHosts.contains(""));
        assertFalse(allowedHosts.contains("0.0.0.0")); // the address of no host
    }

    @Test
    public void refusesAWrongSubnet() {
        for (String subnet : new String[]{"app.example/24", "10.0.0.0/-1", "10.0.0.0/33", "10.0.0.0/x", "::ffff:10.0.0.0/104"})
            try {
                new AllowedHosts(subnet);
                fail(subnet);
            } catch (IllegalArgumentException ignored) {
            }
    }

    @Test
    public void comparesAddressesInTheCanonicalForm() {
        assertEquals("10.0.0.7", AllowedHosts.getCanonical("::ffff:10.0.0.7"));
        assertEquals("::1", AllowedHosts.getCanonical("0:0:0:0:0:0:0:1"));
        assertEquals("fd00::5", AllowedHosts.getCanonical("FD00:0::5"));
        assertEquals("App-2", AllowedHosts.getCanonical("App-2")); // a name as it is
        assertEquals("010.0.0.7", AllowedHosts.getCanonical("010.0.0.7")); // not an address literal
    }

    @Test
    public void containsTheAddressesOfTheSubnets() {
        AllowedHosts allowedHosts = new AllowedHosts("10.0.1.0/24, fd00::/64");
        assertTrue(allowedHosts.contains("10.0.1.5"));
        assertTrue(allowedHosts.contains("fd00::5"));
        assertFalse(allowedHosts.contains("10.0.2.5"));
        assertFalse(allowedHosts.contains("10.0.1.5.other")); // a name, not an address of the subnet
        assertFalse(allowedHosts.contains("١٠.0.1.5")); // digits other than ASCII ones
        assertFalse(new AllowedHosts("0.0.0.0/8").contains("0.0.0.0")); // the address of no host
        assertFalse(new AllowedHosts("").contains("10.0.1.5"));
    }

    @Test
    public void takesOnlyPrivateSubnetsOfTheInterfaces() throws UnknownHostException {
        assertEquals("10.1.2.3/24", AllowedHosts.getLocalSubnet(address("10.1.2.3"), 24));
        assertEquals("172.20.0.5/12", AllowedHosts.getLocalSubnet(address("172.20.0.5"), 4)); // kept within the private range
        assertEquals("fd00::1/64", AllowedHosts.getLocalSubnet(address("fd00::1"), 64));
        assertEquals("fd00::1/7", AllowedHosts.getLocalSubnet(address("fd00::1"), 3));
        assertEquals("10.1.2.3/32", AllowedHosts.getLocalSubnet(address("10.1.2.3"), 40));
        // the address alone: a prefix not known, a loopback address
        assertEquals("10.1.2.3/32", AllowedHosts.getLocalSubnet(address("10.1.2.3"), 0));
        assertEquals("192.168.1.10/32", AllowedHosts.getLocalSubnet(address("192.168.1.10"), -1));
        assertEquals("fd00::1/128", AllowedHosts.getLocalSubnet(address("fd00::1"), 0));
        assertEquals("127.0.0.1/32", AllowedHosts.getLocalSubnet(address("127.0.0.1"), 8));
        assertEquals("::1/128", AllowedHosts.getLocalSubnet(address("::1"), 64));
        assertEquals("fd00::1/64", AllowedHosts.getLocalSubnet(Inet6Address.getByAddress(null, address("fd00::1").getAddress(), 2), 64)); // an interface's address has a scope

        for (String publicAddress : new String[]{"8.8.8.8", "172.32.0.1", "203.0.113.5", "fe80::1", "2001:db8::1"})
            assertNull(publicAddress, AllowedHosts.getLocalSubnet(address(publicAddress), 24));
    }

    @Test
    public void takesTheLoopbackSubnet() throws Exception {
        assertTrue(AllowedHosts.getLocalSubnets().contains("127.0.0.1"));
    }

    private static InetAddress address(String literal) throws UnknownHostException {
        return InetAddress.getByName(literal); // a literal is not resolved
    }
}
