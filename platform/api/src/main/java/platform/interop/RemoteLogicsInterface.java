package platform.interop;

import platform.interop.form.screen.ExternalScreen;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteLogicsInterface extends Remote {

    byte[] findClass(String name) throws RemoteException;

    RemoteNavigatorInterface createNavigator(String login, String password, int computer) throws RemoteException;

    Integer getComputer(String hostname) throws RemoteException;

    ExternalScreen getExternalScreen(int screenID) throws RemoteException;
}
