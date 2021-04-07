package lsfusion.interop.form.remote;

import lsfusion.base.Pair;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.base.remote.RemoteRequestInterface;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.print.FormPrintType;
import lsfusion.interop.form.print.ReportGenerationData;
import lsfusion.interop.form.property.PropertyGroupType;

import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RemoteFormInterface extends RemoteRequestInterface {

    // form structure + design

    byte[] getRichDesignByteArray() throws RemoteException;

    Integer getInitFilterPropertyDraw() throws RemoteException;

    Set<Integer> getInputGroupObjects() throws RemoteException;

    ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, boolean refresh) throws RemoteException;

    // events : form

    ServerResponse gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    ServerResponse setTabVisible(long requestIndex, long lastReceivedRequestIndex, int tabPaneID, int childId) throws RemoteException;

    ServerResponse closedPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException;

    // events : group objects

    ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte[] value) throws RemoteException;

    ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, int groupID, Integer pageSize) throws RemoteException; // размер страницы

    ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte changeType) throws RemoteException; // скроллинг

    ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values) throws RemoteException; // paste подряд

    ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values) throws RemoteException; // paste выборочно
    
    ServerResponse changeMode(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, boolean setGroup, int[] propertyIDs, byte[][] columnKeys, int aggrProps, PropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, UpdateMode updateMode, ListViewType listViewType) throws RemoteException;

    // events : trees

    ServerResponse expandGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, int groupId, boolean current) throws RemoteException;

    ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse collapseGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, int groupId, boolean current) throws RemoteException;

    ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException;

    ServerResponse moveGroupObject(long requestIndex, long lastReceivedRequestIndex, int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException;

    // events : properties

    ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID) throws RemoteException;

    ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, int idNotification) throws RemoteException;

    // async events : properties

    ServerResponse changeProperty(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, byte[] pushChange, Long pushAdd) throws RemoteException;

    // events : filters + orders

    ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException;

    ServerResponse setPropertyOrders(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) throws RemoteException;
    
    ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, byte[][] filters) throws RemoteException;

    ServerResponse setRegularFilter(long requestIndex, long lastReceivedRequestIndex, int groupID, int filterID) throws RemoteException;

    ServerResponse setViewFilters(long requestIndex, long lastReceivedRequestIndex, byte[][] filters, int pageSize) throws RemoteException;

    // group object shortcut actions (system toolbar)

    Object getGroupReportData(long requestIndex, long lastReceivedRequestIndex, Integer groupId, FormPrintType printType, FormUserPreferences userPreferences) throws RemoteException;

    int countRecords(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException;

    Object calculateSum(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKeys) throws RemoteException;

    byte[] groupData(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                              Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException;
    
    List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID) throws RemoteException;
    
    void saveGrouping(long requestIndex, long lastReceivedRequestIndex, FormGrouping grouping) throws RemoteException;

    // user design customization

    ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps) throws RemoteException;

    FormUserPreferences getUserPreferences() throws RemoteException;
    
    ServerResponse refreshUPHiddenProperties(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID, String[] propSids) throws RemoteException;

    // external
    
    Pair<Long, String> changeExternal(long requestIndex, long lastReceivedRequestIndex, String json) throws RemoteException;
}
