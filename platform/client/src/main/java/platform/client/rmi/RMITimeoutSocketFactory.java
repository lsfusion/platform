package platform.client.rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;

public class RMITimeoutSocketFactory extends RMISocketFactory {

    private RMISocketFactory delegateSocketFactory;
    private int timeout;

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
        Socket socket = delegateSocketFactory.createSocket(host, port);
        socket.setSoTimeout(timeout);
        ConnectionLostManager.setConnectionLost(false);
        return socket;
    }

    @Override
    public ServerSocket createServerSocket(int port) throws IOException {
        return delegateSocketFactory.createServerSocket(port);
    }
}
