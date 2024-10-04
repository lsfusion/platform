package lsfusion.interop.connection;

import java.io.Serializable;

public class ComputerInfo implements Serializable {

    public final String hostName;
    public final String hostAddress;

    public ComputerInfo(String hostName, String hostAddress) {
        this.hostName = hostName;
        this.hostAddress = hostAddress;
    }
}
