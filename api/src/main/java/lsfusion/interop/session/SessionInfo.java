package lsfusion.interop.session;

import lsfusion.interop.connection.ConnectionInfo;

import java.io.Serializable;

public class SessionInfo implements Serializable {
    public ExternalRequest externalRequest;
    public ConnectionInfo connectionInfo;

    public SessionInfo(ConnectionInfo connectionInfo, ExternalRequest externalRequest) {
        this.connectionInfo = connectionInfo;
        this.externalRequest = externalRequest;
    }
}
