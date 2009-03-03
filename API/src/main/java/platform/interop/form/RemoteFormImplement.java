package platform.interop.form;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

abstract public class RemoteFormImplement extends UnicastRemoteObject implements RemoteFormInterface {
    
    protected RemoteFormImplement() throws RemoteException {
    }
}
