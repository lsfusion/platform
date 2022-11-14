package lsfusion.interop.connection;

import java.io.Serializable;
import java.util.TimeZone;

public class ConnectionInfo implements Serializable {

    public final String hostName;
    public final String hostAddress;

    public final String language;
    public final String country;
    public final TimeZone timeZone;

    public final String dateFormat;
    public final String timeFormat;

    public ConnectionInfo(String hostName, String hostAddress, String language, String country, TimeZone timeZone, String dateFormat, String timeFormat) {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
        this.language = language;
        this.country = country;
        this.timeZone = timeZone;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }
}
