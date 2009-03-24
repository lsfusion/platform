package platform.interop.form;

import java.sql.SQLException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteFormInterface extends Remote {

    byte[] getReportDesignByteArray() throws RemoteException;
    byte[] getReportDataByteArray() throws RemoteException;

    int getGID() throws RemoteException;

    byte[] getRichDesignByteArray() throws RemoteException;

    void changePageSize(int groupID, int pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    byte[] getFormChangesByteArray() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    int getObjectClassID(int objectID) throws RemoteException;

    void changeGroupObject(int groupID, int changeType) throws RemoteException;

    void changePropertyView(int propertyID, byte[] object, boolean externalID) throws RemoteException;

    void changeObject(int objectID, Integer value) throws RemoteException;

    void addObject(int objectID, int classID) throws RemoteException;

    void changeClass(int objectID, int classID) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    void switchClassView(int groupID) throws RemoteException;

    void changeOrder(int propertyID, int modiType) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    int getID() throws RemoteException;

    void refreshData() throws RemoteException;

    boolean hasSessionChanges() throws RemoteException;

    String saveChanges() throws RemoteException;

    void cancelChanges() throws RemoteException;

    byte[] getBaseClassByteArray(int objectID) throws RemoteException;

    byte[] getChildClassesByteArray(int objectID, int classID) throws RemoteException;

    byte[] getPropertyEditorObjectValueByteArray(int propertyID, boolean externalID) throws RemoteException;

    final static int GID_SHIFT = 1000;

    final static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    final static int CHANGEGROUPOBJECT_LASTROW = 1;
    
    final static int ORDER_REPLACE = 1;
    final static int ORDER_ADD = 2;
    final static int ORDER_REMOVE = 3;
    final static int ORDER_DIR = 4;

}
