package platform.interop.navigator;

import platform.interop.form.RemoteFormInterface;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends Remote {

    String getForms(String formSet) throws RemoteException;

    RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException;

    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    byte[] getElementsByteArray(int groupID) throws RemoteException;

    final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    final static int NAVIGATORGROUP_RELEVANTCLASS = -3;
}
