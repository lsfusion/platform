package platform.interop.form;

import platform.interop.ClassViewType;
import platform.interop.RemoteContextInterface;
import platform.interop.remote.PendingRemote;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemote, RemoteContextInterface {

    // структура формы

    byte[] getRichDesignByteArray() throws RemoteException;

    String getSID() throws RemoteException;

    // синхронное общение с сервером

    public ServerResponse getRemoteChanges() throws RemoteException;

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException;

    public ServerResponse throwInServerInvocation(Exception clientException) throws RemoteException;

    // события формы

    void gainedFocus() throws RemoteException;

    ServerResponse setTabVisible(int tabPaneID, int tabIndex) throws RemoteException;

    ServerResponse closedPressed() throws RemoteException;

    ServerResponse okPressed() throws RemoteException;

    // события групп объектов

    ServerResponse changeGroupObject(int groupID, byte[] value) throws RemoteException;

    ServerResponse changePageSize(int groupID, Integer pageSize) throws RemoteException; // размер страницы

    ServerResponse changeGroupObject(int groupID, byte changeType) throws RemoteException; // скроллинг

    ServerResponse pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws RemoteException; // paste подряд

    ServerResponse pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws RemoteException; // paste выборочно

    ServerResponse changeClassView(int groupID, ClassViewType classView) throws RemoteException;

    // деревья

    ServerResponse expandGroupObject(int groupId, byte[] bytes) throws RemoteException;

    ServerResponse collapseGroupObject(int groupId, byte[] bytes) throws RemoteException;

    ServerResponse moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    // свойства
    ServerResponse executeEditAction(int propertyID, byte[] columnKey, String actionSID) throws RemoteException;

    ServerResponse changeProperty(int propertyID, byte[] fullKey, byte[] value) throws RemoteException;

    // фильтры / порядки

    ServerResponse changeGridClass(int objectID, int idClass) throws RemoteException;

    ServerResponse changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    ServerResponse setUserFilters(byte[][] filters) throws RemoteException;

    ServerResponse setRegularFilter(int groupID, int filterID) throws RemoteException;

    // отчеты

    byte[] getReportHierarchyByteArray() throws RemoteException;
    byte[] getSingleGroupReportHierarchyByteArray(int groupId) throws RemoteException;

    byte[] getReportDesignsByteArray(boolean toExcel, FormUserPreferences userPreferences) throws RemoteException;
    byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId, FormUserPreferences userPreferences) throws RemoteException;
    byte[] getReportSourcesByteArray() throws RemoteException;
    byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException;
    Map<String, String> getReportPath(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    // быстрая информация

    int countRecords(int groupObjectID) throws RemoteException;

    Object calculateSum(int propertyID, byte[] columnKeys) throws RemoteException;

    Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;

    // пользовательские настройки

    void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException;

    FormUserPreferences loadUserPreferences() throws RemoteException;
}
