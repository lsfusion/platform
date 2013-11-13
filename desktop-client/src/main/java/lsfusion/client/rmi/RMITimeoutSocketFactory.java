package lsfusion.client.rmi;

import lsfusion.base.WeakLinkedHashSet;
import lsfusion.interop.remote.CountZipSocket;
import lsfusion.interop.remote.ISocketTrafficSum;
import lsfusion.interop.remote.ZipSocketFactory;

import java.io.IOException;

public class RMITimeoutSocketFactory extends ZipSocketFactory implements ISocketTrafficSum {

    private final int timeout;

    private transient final WeakLinkedHashSet<CountZipSocket> sockets = new WeakLinkedHashSet<CountZipSocket>();

    public transient long inSum;
    public transient long outSum;

    public RMITimeoutSocketFactory(int timeout) {
        this.timeout = timeout;

        String timeoutValue = String.valueOf(timeout);
        // официально не поддерживаемые свойства rmi
        // http://download.oracle.com/javase/6/docs/technotes/guides/rmi/sunrmiproperties.html
        System.setProperty("sun.rmi.transport.tcp.readTimeout", timeoutValue);
        System.setProperty("sun.rmi.transport.proxy.connectTimeout", timeoutValue);
    }

    @Override
    public CountZipSocket createSocket(String host, int port) throws IOException {
        CountZipSocket socket = super.createSocket(host, port);
        socket.setObserver(this);
        socket.setSoTimeout(timeout);

        sockets.add(socket);

        ConnectionLostManager.connectionRelived();

        return socket;
    }

    public void incrementIn(long in) {
        inSum += in;
    }

    public void incrementOut(long out) {
        outSum += out;
    }

    public void closeHangingSockets() {
        for (CountZipSocket socket : sockets) {
            if (socket != null) {
                socket.closeIfHung();
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        RMITimeoutSocketFactory that = (RMITimeoutSocketFactory) o;

        return timeout == that.timeout;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + timeout;
        return result;
    }
}
