package platform.interop.form;

import platform.interop.action.ClientApply;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;

public interface RemoteFormInterface extends PendingRemote {

    // операции с ответом
    byte[] getReportDesignByteArray(boolean toExcel) throws RemoteException;
    boolean hasCustomReportDesign() throws RemoteException;
    byte[] getRichDesignByteArray() throws RemoteException;

    byte[] getReportDataByteArray() throws RemoteException;

    public RemoteChanges getRemoteChanges() throws RemoteException;

    int getID() throws RemoteException;

    // синхронная проверка на то можно ли менять свойство
    byte[] getPropertyChangeType(int propertyID) throws RemoteException;
    boolean canChangeClass(int objectID) throws RemoteException;

    boolean hasClientApply() throws RemoteException; // чисто для оптимизации одного RMI вызова

    RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException;

    RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException;

    RemoteDialogInterface createObjectDialog(int objectID) throws RemoteException;

    RemoteDialogInterface createObjectDialogWithValue(int objectID, int value) throws RemoteException;

    // операции без ответа, можно pendiть до первой операции с ответом

    void changePageSize(int groupID, int pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    void changePropertyDraw(int propertyID, byte[] object, boolean all) throws RemoteException;

    void changePropertyDrawWithColumnKeys(int propertyID, byte[] object, boolean all, byte[] columnKeys) throws RemoteException;

    void changeClass(int objectID, int classID) throws RemoteException;

    void addObject(int objectID, int classID) throws RemoteException;

    void switchClassView(int groupID) throws RemoteException;

    void changeClassView(int groupID, byte classView) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType) throws RemoteException;

    void changePropertyOrderWithColumnKeys(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    void refreshData() throws RemoteException;

    void cancelChanges() throws RemoteException;

    ClientApply applyClientChanges() throws RemoteException;
    void confirmClientChanges(Object clientResult) throws RemoteException;
    void rollbackClientChanges() throws RemoteException;
    void applyChanges() throws RemoteException;
    
    final static int GID_SHIFT = 1000;

    final static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    final static int CHANGEGROUPOBJECT_LASTROW = 1;
}
