package platform.interop;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public class RemoteObject extends UnicastRemoteObject {

    final protected int exportPort;

    public RemoteObject(int port) throws RemoteException {
        super(port);

        exportPort = port;
    }
}
