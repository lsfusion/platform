package lsfusion.base.remote;

import java.io.IOException;
import java.rmi.server.RMIServerSocketFactory;

public class ZipServerSocketFactory extends ZipSocketFactory implements RMIServerSocketFactory {
    public static final ZipServerSocketFactory instance = new ZipServerSocketFactory();

    public ZipServerSocket createServerSocket(int port) throws IOException {
        return new ZipServerSocket(port);
    }

}
