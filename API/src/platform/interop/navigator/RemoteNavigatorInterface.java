package platform.interop.navigator;

import platform.interop.form.RemoteFormInterface;

import java.sql.SQLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteNavigatorInterface extends Remote {

    public void addCacheObject(int classID, int value) throws RemoteException;

    int getDefaultForm(int classId) throws RemoteException;

    int getCacheObject(int classID) throws RemoteException;

    public RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException;

    byte[] getCurrentUserInfoByteArray() throws RemoteException;

    byte[] getElementsByteArray(int groupID) throws RemoteException;

    boolean changeCurrentClass(int classID) throws RemoteException;

    String getCaption(int formID) throws RemoteException;
    
    public final static int NAVIGATORGROUP_RELEVANTFORM = -2;
    public final static int NAVIGATORGROUP_RELEVANTCLASS = -3;
}
