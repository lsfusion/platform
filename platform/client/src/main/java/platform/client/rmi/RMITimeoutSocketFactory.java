package platform.client.rmi;

import platform.base.WeakLinkedHashSet;
import platform.interop.remote.CountZipServerSocket;
import platform.interop.remote.CountZipSocket;
import platform.interop.remote.ISocketTrafficSum;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;
import java.util.Iterator;

public class RMITimeoutSocketFactory extends RMISocketFactory implements ISocketTrafficSum{

    private int timeout;
    public long inSum;
    public long outSum;
    public static WeakLinkedHashSet<CountZipSocket> sockets = new WeakLinkedHashSet<CountZipSocket>();

    public RMITimeoutSocketFactory(int timeout) {
        this.timeout = timeout;

        String timeoutValue = String.valueOf(timeout);
        // официально не поддерживаемые свойства rmi
        // http://download.oracle.com/javase/6/docs/technotes/guides/rmi/sunrmiproperties.html
        System.setProperty("sun.rmi.transport.tcp.readTimeout", timeoutValue);
        System.setProperty("sun.rmi.transport.proxy.connectTimeout", timeoutValue);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        CountZipSocket socket = new CountZipSocket(host, port);
        socket.setObserver(this);
        socket.setSoTimeout(timeout);
        ConnectionLostManager.connectionRelived();
        sockets.add(socket);
        return socket;
    }

    public static void closeHangingSockets() {
        Iterator<CountZipSocket> iterator = sockets.iterator();
        while (iterator.hasNext()) {
            CountZipSocket socket = iterator.next();
            if (socket != null) {
                socket.closeIfHung();
            }
        }
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return new CountZipServerSocket(port);
    }

    public void incrementIn(long in) {
        inSum += in;
    }

    public void incrementOut(long out) {
        outSum += out;
    }
}
