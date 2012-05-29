package platform.interop.form;

import platform.interop.ClassViewType;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemote, RemoteContextInterface {

    // операции с ответом
    byte[] getRichDesignByteArray() throws RemoteException;

    byte[] getReportHierarchyByteArray() throws RemoteException;

    byte[] getSingleGroupReportHierarchyByteArray(int groupId) throws RemoteException;

    byte[] getReportDesignsByteArray(boolean toExcel, FormUserPreferences userPreferences) throws RemoteException;

    byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId, FormUserPreferences userPreferences) throws RemoteException;

    byte[] getReportSourcesByteArray() throws RemoteException;

    byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException;

    Map<String, String> getReportPath(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    public ServerResponse getRemoteChanges() throws RemoteException;

    String getSID() throws RemoteException;

    public ServerResponse executeEditAction(int propertyID, byte[] columnKey, String actionSID) throws RemoteException;

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException;

    public ServerResponse throwInServerInvocation(Exception clientException) throws RemoteException;

    boolean canChangeClass(int objectID) throws RemoteException;

    // операции без ответа, можно pendiть до первой операции с ответом

    void changePageSize(int groupID, Integer pageSize) throws RemoteException;

    void gainedFocus() throws RemoteException;

    void changeGroupObject(int groupID, byte[] value) throws RemoteException;

    void changeGroupObject(int groupID, byte changeType) throws RemoteException;

    ServerResponse pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws RemoteException;

    ServerResponse pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws RemoteException;

    void changeClassView(int groupID, ClassViewType classView) throws RemoteException;

    void changeGridClass(int objectID, int idClass) throws RemoteException;

    void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    void clearUserFilters() throws RemoteException;

    void addFilter(byte[] state) throws RemoteException;

    void setRegularFilter(int groupID, int filterID) throws RemoteException;

    int countRecords(int groupObjectID) throws RemoteException;

    Object calculateSum(int propertyID, byte[] columnKeys) throws RemoteException;

    Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;

    void expandGroupObject(int groupId, byte[] bytes) throws RemoteException;

    void collapseGroupObject(int groupId, byte[] bytes) throws RemoteException;

    void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    ServerResponse closedPressed() throws RemoteException;

    ServerResponse okPressed() throws RemoteException;

    void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException;

    FormUserPreferences loadUserPreferences() throws RemoteException;
}
