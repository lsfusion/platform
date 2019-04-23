package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

public class SessionInfo extends ConnectionInfo {
    public String query;

    public SessionInfo(String hostName, String hostAddress, String language, String country) {
        this(hostName, hostAddress, language, country, null);
    }

    public SessionInfo(String hostName, String hostAddress, String language, String country, String query) {
        super(hostName, hostAddress, language, country);
        this.query = query;
    }
}
