package lsfusion.server.base.controller.remote;

import lsfusion.base.remote.CountZipServerSocket;
import lsfusion.base.remote.ZipSocketFactory;

import java.io.IOException;
import java.rmi.server.RMIServerSocketFactory;

public class ZipServerSocketFactory extends ZipSocketFactory implements RMIServerSocketFactory {
    private static final ZipServerSocketFactory instance = new ZipServerSocketFactory();

    public static ZipServerSocketFactory getInstance() {
        return instance;
    }

    public CountZipServerSocket createServerSocket(int port) throws IOException {
        return new CountZipServerSocket(port);
    }

}
