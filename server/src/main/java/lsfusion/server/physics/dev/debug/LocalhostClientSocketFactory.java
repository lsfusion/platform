package lsfusion.server.physics.dev.debug;

import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;

public class LocalhostClientSocketFactory implements RMIClientSocketFactory, Serializable {

    public LocalhostClientSocketFactory() {
    }

    public Socket createSocket(String host, int port) throws IOException {
        return new Socket("localhost", port);
    }
}