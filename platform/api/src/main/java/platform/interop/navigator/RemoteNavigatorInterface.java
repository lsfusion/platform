package platform.interop.navigator;

import platform.interop.form.RemoteFormInterface;

import java.sql.SQLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends Remote {

    RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException;

    // диалоги
    RemoteFormInterface createObjectForm(int objectID) throws RemoteException;
    RemoteFormInterface createObjectForm(int objectID, int value) throws RemoteException;
    RemoteFormInterface createPropertyForm(int viewID, int value) throws RemoteException;
    RemoteFormInterface createChangeForm(int viewID) throws RemoteException;

    int getDialogObject() throws RemoteException;

    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    byte[] getElementsByteArray(int groupID) throws RemoteException;

    boolean changeCurrentClass(int classID) throws RemoteException;

    String getCaption(int formID) throws RemoteException;
    
    final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    final static int NAVIGATORGROUP_RELEVANTCLASS = -3;
}
