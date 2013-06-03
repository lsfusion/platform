package lsfusion.interop.remote;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;

public class ZipSocketFactory extends RMISocketFactory implements RMIServerSocketFactory, Serializable {
    private String overrideHostName;

    public void setOverrideHostName(String overrideHostName) {
        this.overrideHostName = overrideHostName;
    }

    public CountZipSocket createSocket(String host, int port) throws IOException {
        if (overrideHostName != null) {
            host = overrideHostName;
        }
        return new CountZipSocket(host, port);
    }

    public CountZipServerSocket createServerSocket(int port) throws IOException {
        return new CountZipServerSocket(port);
    }
}
