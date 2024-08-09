package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

import java.util.TimeZone;

public class SessionInfo extends ConnectionInfo {
    public ExternalRequest externalRequest;

    public SessionInfo(String hostName, String hostAddress, String language, String country, TimeZone timeZone, String dateFormat, String timeFormat, String clientColorTheme) {
        this(hostName, hostAddress, language, country, timeZone, dateFormat, timeFormat, clientColorTheme, new ExternalRequest(new ExternalRequest.Param[0]));
    }

    public SessionInfo(String hostName, String hostAddress, String language, String country, TimeZone timeZone, String dateFormat, String timeFormat, String clientColorTheme,
                       ExternalRequest externalRequest) {
        super(hostName, hostAddress, language, country, timeZone, dateFormat, timeFormat, clientColorTheme);
        this.externalRequest = externalRequest;
    }
}
