package platform.server.net;

import platform.interop.remote.CountZipServerSocket;
import platform.interop.remote.CountZipSocket;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMISocketFactory;


import java.io.Serializable;
import java.rmi.server.RMIServerSocketFactory;

public class ServerSocketFactory extends RMISocketFactory implements RMIServerSocketFactory, Serializable {

    public Socket createSocket(String host, int port) throws IOException {
        Socket socket = new CountZipSocket(host, port);
        return socket;
    }

    public ServerSocket createServerSocket(int port) throws IOException {
        CountZipServerSocket server = new CountZipServerSocket(port);
        return server;
    }
}
