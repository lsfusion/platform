package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

public class SessionInfo extends ConnectionInfo {

    public SessionInfo(String hostName, String hostAddress, String language, String country) {
        super(hostName, hostAddress, language, country);
    }
}
