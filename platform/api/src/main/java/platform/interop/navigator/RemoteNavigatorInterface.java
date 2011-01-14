package platform.interop.navigator;

import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends PendingRemote {

    String getForms(String formSet) throws RemoteException;

    RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException;
    RemoteFormInterface createForm(byte[] formState) throws RemoteException;
    void saveForm(int formID, byte[] formState) throws RemoteException;
    void saveVisualSetup(byte[] data) throws RemoteException;

    byte[] getRichDesignByteArray(int formID) throws RemoteException;
    byte[] getFormEntityByteArray(int formID) throws RemoteException;

    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    byte[] getElementsByteArray(int groupID) throws RemoteException;

    void relogin(String login) throws RemoteException;
    
    void clientExceptionLog(String info) throws RemoteException;

    final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    final static int NAVIGATORGROUP_RELEVANTCLASS = -3;

    void close() throws RemoteException;
}
