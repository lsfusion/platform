package platform.client.rmi;

import platform.interop.remote.CountZipSocket;
import platform.interop.remote.ISocketTrafficSum;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

public class RMITimeoutSocketFactory extends RMISocketFactory implements ISocketTrafficSum{

    private RMISocketFactory delegateSocketFactory;
    private int timeout;
    public long inSum;
    public long outSum;

    public RMITimeoutSocketFactory(RMISocketFactory delegateSocketFactory, int timeout) {
        this.delegateSocketFactory = delegateSocketFactory;
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
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return delegateSocketFactory.createServerSocket(port);
    }

    public void incrementIn(long in) {
        inSum += in;
    }

    public void incrementOut(long out) {
        outSum += out;
    }
}
