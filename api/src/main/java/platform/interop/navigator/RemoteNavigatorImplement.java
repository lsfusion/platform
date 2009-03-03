package platform.interop.navigator;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

abstract public class RemoteNavigatorImplement extends UnicastRemoteObject implements RemoteNavigatorInterface {

    protected RemoteNavigatorImplement() throws RemoteException {
    }
}
