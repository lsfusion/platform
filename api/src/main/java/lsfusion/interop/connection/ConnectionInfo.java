package lsfusion.interop.connection;

import java.io.Serializable;

public class ConnectionInfo implements Serializable {

    public final String hostName;
    public final String hostAddress;

    public final String language;
    public final String country;

    public final String dateFormat;
    public final String timeFormat;

    public ConnectionInfo(String hostName, String hostAddress, String language, String country, String dateFormat, String timeFormat) {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
        this.language = language;
        this.country = country;
        this.dateFormat = dateFormat;
        this.timeFormat = timeFormat;
    }
}
