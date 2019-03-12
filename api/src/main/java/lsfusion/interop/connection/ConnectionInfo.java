package lsfusion.interop.connection;

import java.io.Serializable;

public class ConnectionInfo implements Serializable {

    public final String hostName;
    public final String hostAddress;

    public final String language;
    public final String country;

    public ConnectionInfo(String hostName, String hostAddress, String language, String country) {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
        this.language = language;
        this.country = country;
    }
}
