package lsfusion.gwt.server.logics;

import java.util.Objects;

public class LogicsConnection {

    public final String host;
    public final int port;
    public final String exportName;

    public LogicsConnection(String host, int port, String exportName) {
        this.host = host;
        this.port = port;
        this.exportName = exportName;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof LogicsConnection && port == ((LogicsConnection) o).port && host.equals(((LogicsConnection) o).host) && exportName.equals(((LogicsConnection) o).exportName);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * host.hashCode() + port) + exportName.hashCode();
    }
}
