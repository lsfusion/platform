package platform.interop;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

public abstract class RemoteLogicsImplement extends UnicastRemoteObject implements RemoteLogicsInterface {
    
    protected RemoteLogicsImplement() throws RemoteException {
    }
}
