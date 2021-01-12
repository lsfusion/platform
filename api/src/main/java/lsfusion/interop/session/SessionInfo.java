package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

public class SessionInfo extends ConnectionInfo {
    public String query;

    public SessionInfo(String hostName, String hostAddress, String language, String country, String dateFormat, String timeFormat) {
        this(hostName, hostAddress, language, country, dateFormat, timeFormat, null);
    }

    public SessionInfo(String hostName, String hostAddress, String language, String country, String dateFormat, String timeFormat, String query) {
        super(hostName, hostAddress, language, country, dateFormat, timeFormat);
        this.query = query;
    }
}
