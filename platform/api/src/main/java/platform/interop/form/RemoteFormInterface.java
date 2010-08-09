package platform.interop.form;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RemoteFormInterface extends Remote {

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

    boolean switchClassView(int groupID) throws RemoteException;

    boolean changeClassView(int groupID, byte classView) throws RemoteException;

    List<? extends ClientAction> getApplyActions() throws RemoteException;

    String checkApplyActions(int actionID, ClientActionResult result) throws RemoteException;

    String checkChanges() throws RemoteException;

    String applyChanges() throws RemoteException;

    RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException;

    RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException;

    RemoteDialogInterface createObjectDialog(int objectID) throws RemoteException;

    RemoteDialogInterface createObjectDialog(int objectID, int value) throws RemoteException;
    
    // операции без ответа, можно pendiть до первой операции с ответом
    
    void changePageSize(int groupID, int pageSize) throws RemoteException;
    void gainedFocus() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    void changePropertyView(int propertyID, byte[] object, boolean all) throws RemoteException;

    void changeObject(int objectID, Object value) throws RemoteException;

    void changeClass(int objectID, int classID) throws RemoteException;
    
    void addObject(int objectID, int classID) throws RemoteException;

    void changeGridClass(int objectID,int idClass) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType) throws RemoteException;

    void changeObjectOrder(int propertyID, byte modiType) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    void refreshData() throws RemoteException;

    void cancelChanges() throws RemoteException;

    final static int GID_SHIFT = 1000;

    final static int CHANGEGROUPOBJECT_FIRSTROW = 0;
    final static int CHANGEGROUPOBJECT_LASTROW = 1;
}
