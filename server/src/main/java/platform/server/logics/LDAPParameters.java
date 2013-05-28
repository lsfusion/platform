package platform.server.logics;

import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LDAPParameters {

    private boolean connected;
    private String groupName;

    public LDAPParameters(boolean connected) {
        this(connected, null);
    }

    public LDAPParameters(boolean connected, String groupName) {
        this.connected = connected;
        this.groupName = groupName;
    }

    public boolean isConnected() {
        return connected;
    }

    public String getGroupName() {
        return groupName;
    }
}
