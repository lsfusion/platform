package lsfusion.interop;

import java.io.Serializable;

public class ServerInfo implements Serializable {
    private String name;
    private String hostName;
    private int port;

    public ServerInfo(String name, String hostName, int port) {
        this.name = name;
        this.hostName = hostName;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        StringBuilder fullName = new StringBuilder(name);
        fullName.append(" (");
        fullName.append(hostName);
        fullName.append(":");
        fullName.append(port);
        fullName.append(")");
        return fullName.toString();
    }
}
