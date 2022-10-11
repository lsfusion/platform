package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

import java.util.TimeZone;

public class SessionInfo extends ConnectionInfo {
    public ExternalRequest externalRequest;

    public SessionInfo(String hostName, String hostAddress, String language, String country, TimeZone timeZone, String dateFormat, String timeFormat) {
        this(hostName, hostAddress, language, country, timeZone, dateFormat, timeFormat, new ExternalRequest());
    }

    public SessionInfo(String hostName, String hostAddress, String language, String country, TimeZone timeZone, String dateFormat, String timeFormat, ExternalRequest externalRequest) {
        super(hostName, hostAddress, language, country, timeZone, dateFormat, timeFormat);
        this.externalRequest = externalRequest;
    }
}
