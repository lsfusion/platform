package lsfusion.interop.form;

import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormGrouping;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface RemoteFormInterface extends PendingRemoteInterface {

    // структура формы

    byte[] getRichDesignByteArray() throws RemoteException;

    Integer getInitFilterPropertyDraw() throws RemoteException;

    // синхронное общение с сервером

    ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, boolean refresh) throws RemoteException;

    ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException;

    ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException;

    boolean isInServerInvocation(long requestIndex) throws RemoteException;

    void interrupt(boolean cancelable) throws RemoteException;

    // события формы

    void gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    ServerResponse setTabVisible(long requestIndex, long lastReceivedRequestIndex, int tabPaneID, int childId) throws RemoteException;

    ServerResponse closedPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    ServerResponse okPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    // события групп объектов

    ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte[] value) throws RemoteException;

    ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, int groupID, Integer pageSize) throws RemoteException; // размер страницы

    ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte changeType) throws RemoteException; // скроллинг

    ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values) throws RemoteException; // paste подряд

    ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values) throws RemoteException; // paste выборочно

    ServerResponse changeClassView(long requestIndex, long lastReceivedRequestIndex, int groupID, ClassViewType classView) throws RemoteException;

    // деревья

    ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse moveGroupObject(long requestIndex, long lastReceivedRequestIndex, int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    // свойства

    ServerResponse executeEditAction(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID) throws RemoteException;

    ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, int idNotification) throws RemoteException;

    // асинхронные вызовы

    ServerResponse changeProperty(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, byte[] pushChange, Integer pushAdd) throws RemoteException;

    // фильтры / порядки

    ServerResponse changeGridClass(long requestIndex, long lastReceivedRequestIndex, int objectID, int idClass) throws RemoteException;

    ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    ServerResponse clearPropertyOrders(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException;
    
    ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, byte[][] filters) throws RemoteException;

    ServerResponse setRegularFilter(long requestIndex, long lastReceivedRequestIndex, int groupID, int filterID) throws RemoteException;

    // отчеты

    ReportGenerationData getReportData(long requestIndex, long lastReceivedRequestIndex, Integer groupId, boolean toExcel, FormUserPreferences userPreferences) throws RemoteException;

    Map<String, String> getReportPath(long requestIndex, long lastReceivedRequestIndex, boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException;

    // быстрая информация

    int countRecords(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException;

    Object calculateSum(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKeys) throws RemoteException;

    byte[] groupData(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;
    
    List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID) throws RemoteException;
    
    void saveGrouping(long requestIndex, long lastReceivedRequestIndex, FormGrouping grouping) throws RemoteException;

    // пользовательские настройки

    ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride) throws RemoteException;

    FormUserPreferences getUserPreferences() throws RemoteException;
    
    ColorPreferences getColorPreferences() throws RemoteException;
}
