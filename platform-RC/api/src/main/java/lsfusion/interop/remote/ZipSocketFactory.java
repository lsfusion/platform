package lsfusion.interop.remote;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;

public class ZipSocketFactory extends RMISocketFactory implements RMIServerSocketFactory, Serializable {
    private static final ZipSocketFactory instance = new ZipSocketFactory();

    public static ZipSocketFactory getInstance() {
        return instance;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ZipSocketFactory that = (ZipSocketFactory) o;

        return !(overrideHostName == null ? that.overrideHostName != null : !overrideHostName.equals(that.overrideHostName));
    }

    @Override
    public int hashCode() {
        return overrideHostName != null ? overrideHostName.hashCode() : 0;
    }
}
