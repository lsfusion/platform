package platform.interop;

import platform.interop.navigator.RemoteNavigatorInterface;
import platform.base.Pair;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface RemoteLogicsInterface extends Remote {

    RemoteNavigatorInterface createNavigator(String login,String password,int computer) throws RemoteException;

    Collection<Integer> getComputers() throws RemoteException;
}
