package platform.interop;

import platform.interop.form.screen.ExternalScreen;
import platform.interop.form.screen.ExternalScreenParameters;
import platform.interop.navigator.RemoteNavigatorInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsInterface extends PendingRemote {

    String getName() throws RemoteException;

    byte[] findClass(String name) throws RemoteException;

    RemoteNavigatorInterface createNavigator(String login, String password, int computer) throws RemoteException;

    Integer getComputer(String hostname) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;

    ExternalScreenParameters getExternalScreenParameters(int screenID, int computerId) throws RemoteException;
}
