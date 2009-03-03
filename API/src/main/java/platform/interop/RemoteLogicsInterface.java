package platform.interop;

import platform.interop.navigator.RemoteNavigatorInterface;
import platform.base.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsInterface extends Remote {

    RemoteNavigatorInterface createNavigator(String login,String password) throws RemoteException;
}
