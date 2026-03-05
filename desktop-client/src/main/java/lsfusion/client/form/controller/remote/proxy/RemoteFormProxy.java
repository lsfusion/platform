package lsfusion.client.form.controller.remote.proxy;

import lsfusion.base.Pair;
import lsfusion.client.controller.remote.proxy.RemoteRequestObjectProxy;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.UpdateMode;
import lsfusion.interop.form.event.FormEvent;
import lsfusion.interop.form.object.table.grid.ListViewType;
import lsfusion.interop.form.object.table.grid.user.design.FormUserPreferences;
import lsfusion.interop.form.object.table.grid.user.design.GroupObjectUserPreferences;
import lsfusion.interop.form.object.table.grid.user.toolbar.FormGrouping;
import lsfusion.interop.form.property.EventSource;
import lsfusion.interop.form.property.PropertyGroupType;
import lsfusion.interop.form.remote.RemoteFormInterface;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteFormProxy extends RemoteRequestObjectProxy<RemoteFormInterface> implements RemoteFormInterface {

    public static final Map<String, byte[]> cachedRichDesign = new HashMap<>();
    public static void dropCaches() {
        cachedRichDesign.clear();
    }

    public RemoteFormProxy(RemoteFormInterface target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public Pair<Long, String> changeExternal(long requestIndex, long lastReceivedRequestIndex, String json) throws RemoteException {
        logRemoteMethodStartVoidCall("changeExternal");
        Pair<Long, String> result = target.changeExternal(requestIndex, lastReceivedRequestIndex, json);
        logRemoteMethodEndVoidCall("changeExternal");
        return result;
    }

    public Object getGroupReportData(long requestIndex, long lastReceivedRequestIndex, Integer groupId, FormUserPreferences userPreferences) throws RemoteException {
        return target.getGroupReportData(requestIndex, lastReceivedRequestIndex, groupId, userPreferences);
    }

    public ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, boolean refresh, boolean forceLocalEvents) throws RemoteException {
        logRemoteMethodStartCall("getRemoteChanges");
        ServerResponse result = target.getRemoteChanges(requestIndex, lastReceivedRequestIndex, refresh, forceLocalEvents);
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    public ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, int groupID, Integer pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("changePageSize");
        ServerResponse result = target.changePageSize(requestIndex, lastReceivedRequestIndex, groupID, pageSize);
        logRemoteMethodEndVoidCall("changePageSize");
        return result;
    }

    public ServerResponse voidFormAction(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("voidFormAction");
        ServerResponse result = target.voidFormAction(requestIndex, lastReceivedRequestIndex);
        logRemoteMethodEndVoidCall("voidFormAction");
        return result;
    }

    public ServerResponse gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("gainedFocus");
        ServerResponse result = target.gainedFocus(requestIndex, lastReceivedRequestIndex);
        logRemoteMethodEndVoidCall("gainedFocus");
        return result;
    }

    public ServerResponse setTabActive(long requestIndex, long lastReceivedRequestIndex, int tabPaneID, int childId) throws RemoteException {
        logRemoteMethodStartVoidCall("setTabActive");
        ServerResponse result = target.setTabActive(requestIndex, lastReceivedRequestIndex, tabPaneID, childId);
        logRemoteMethodEndVoidCall("setTabActive");
        return result;
    }

    public ServerResponse setPropertyActive(long requestIndex, long lastReceivedRequestIndex, int propertyID, boolean focused) throws RemoteException {
        logRemoteMethodStartVoidCall("setPropertyActive");
        ServerResponse result = target.setPropertyActive(requestIndex, lastReceivedRequestIndex, propertyID, focused);
        logRemoteMethodEndVoidCall("setPropertyActive");
        return result;
    }

    public ServerResponse setContainerCollapsed(long requestIndex, long lastReceivedRequestIndex, int containerID, boolean collapsed) throws RemoteException {
        logRemoteMethodStartVoidCall("setContainerCollapsed");
        ServerResponse result = target.setContainerCollapsed(requestIndex, lastReceivedRequestIndex, containerID, collapsed);
        logRemoteMethodEndVoidCall("setContainerCollapsed");
        return result;
    }

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte[] value) throws RemoteException {
        logRemoteMethodStartCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(requestIndex, lastReceivedRequestIndex, groupID, value);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    public ServerResponse changeGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupID, byte changeType) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(requestIndex, lastReceivedRequestIndex, groupID, changeType);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    @Override
    public ServerResponse changeMode(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, boolean setGroup, int[] propertyIDs, byte[][] columnKeys, int aggrProps, PropertyGroupType aggrType, Integer pageSize, boolean forceRefresh, UpdateMode updateMode, ListViewType viewType) throws RemoteException {
        logRemoteMethodStartVoidCall("changeMode");
        ServerResponse result = target.changeMode(requestIndex, lastReceivedRequestIndex, groupObjectID, setGroup, propertyIDs, columnKeys, aggrProps, aggrType, pageSize, forceRefresh, updateMode, viewType);
        logRemoteMethodEndVoidCall("changeMode");
        return result;
    }

    public ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values, List<ArrayList<String>> rawValues) throws RemoteException {
        logRemoteMethodStartCall("pasteExternalTable");
        ServerResponse result = target.pasteExternalTable(requestIndex, lastReceivedRequestIndex, propertyIDs, columnKeys, values, rawValues);
        logRemoteMethodEndCall("pasteExternalTable", result);
        return result;
    }

    public ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values, Map<Integer, String> rawValues) throws RemoteException {
        logRemoteMethodStartCall("pasteMulticellValue");
        ServerResponse result = target.pasteMulticellValue(requestIndex, lastReceivedRequestIndex, keys, values, rawValues);
        logRemoteMethodEndCall("pasteMulticellValue", result);
        return result;
    }

    public ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        ServerResponse result = target.changePropertyOrder(requestIndex, lastReceivedRequestIndex, propertyID, modiType, columnKeys);
        logRemoteMethodEndCall("changePropertyOrder", result);
        return result;
    }

    @Override
    public ServerResponse setPropertyOrders(long requestIndex, long lastReceivedRequestIndex, int groupObjectID, List<Integer> propertyList, List<byte[]> columnKeyList, List<Boolean> orderList) throws RemoteException {
        logRemoteMethodStartVoidCall("setPropertyOrders");
        ServerResponse result = target.setPropertyOrders(requestIndex, lastReceivedRequestIndex, groupObjectID, propertyList, columnKeyList, orderList);
        logRemoteMethodEndCall("setPropertyOrders", result);
        return result;
    }

    public ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, Map<Integer, byte[][]> filters) throws RemoteException {
        logRemoteMethodStartVoidCall("setUserFilters");
        ServerResponse result = target.setUserFilters(requestIndex, lastReceivedRequestIndex, filters);
        logRemoteMethodEndCall("setUserFilters", result);
        return result;
    }

    public ServerResponse setRegularFilter(long requestIndex, long lastReceivedRequestIndex, int groupID, int filterID) throws RemoteException {
        logRemoteMethodStartVoidCall("setRegularFilter");
        ServerResponse result = target.setRegularFilter(requestIndex, lastReceivedRequestIndex, groupID, filterID);
        logRemoteMethodEndCall("setRegularFilter", result);
        return result;
    }

    public ServerResponse setViewFilters(long requestIndex, long lastReceivedRequestIndex, final byte[][] filters, int pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("setViewFilter");
        ServerResponse result = target.setViewFilters(requestIndex, lastReceivedRequestIndex, filters, pageSize);
        logRemoteMethodEndCall("setViewFilter", result);
        return result;
    }

    public int countRecords(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException {
        logRemoteMethodStartCall("countRecords");
        int result = target.countRecords(requestIndex, lastReceivedRequestIndex, groupObjectID);
        logRemoteMethodEndCall("countRecords", result);
        return result;
    }

    public Object calculateSum(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartCall("calculateSum");
        Object result = target.calculateSum(requestIndex, lastReceivedRequestIndex, propertyID, columnKeys);
        logRemoteMethodEndCall("calculateSum", result);
        return result;
    }

    public byte[] groupData(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                                     Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException {
        logRemoteMethodStartCall("groupData");
        byte[] result = target.groupData(requestIndex, lastReceivedRequestIndex, groupMap, sumMap, maxMap, onlyNotNull);
        logRemoteMethodEndCall("groupData", result);
        return result;
    }

    @Override
    public List<FormGrouping> readGroupings(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID) throws RemoteException {
        logRemoteMethodStartCall("readGroupings");
        List<FormGrouping> result = target.readGroupings(requestIndex, lastReceivedRequestIndex, groupObjectSID);
        logRemoteMethodEndVoidCall("readGroupings");
        return result;
    }

    @Override
    public void saveGrouping(long requestIndex, long lastReceivedRequestIndex, FormGrouping grouping) throws RemoteException {
        logRemoteMethodStartCall("saveGrouping");
        target.saveGrouping(requestIndex, lastReceivedRequestIndex, grouping);
        logRemoteMethodEndVoidCall("saveGrouping");   
    }

    public ServerResponse saveUserPreferences(long requestIndex, long lastReceivedRequestIndex, GroupObjectUserPreferences preferences, boolean forAllUsers, boolean completeOverride, String[] hiddenProps) throws RemoteException {
        logRemoteMethodStartCall("saveUserPreferences");
        ServerResponse result = target.saveUserPreferences(requestIndex, lastReceivedRequestIndex, preferences, forAllUsers, completeOverride, hiddenProps);
        logRemoteMethodEndCall("saveUserPreferences", result);
        return result;
    }

    @Override
    public ServerResponse refreshUPHiddenProperties(long requestIndex, long lastReceivedRequestIndex, String groupObjectSID, String[] propSids) throws RemoteException {
        logRemoteMethodStartCall("refreshUPHiddenProperties");
        ServerResponse result = target.refreshUPHiddenProperties(requestIndex, lastReceivedRequestIndex, groupObjectSID, propSids);
        logRemoteMethodEndVoidCall("refreshUPHiddenProperties");
        return result;
    }

    @Override
    public ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, FormEvent formEvent, byte[] pushAsyncResult) throws RemoteException {
        logRemoteMethodStartCall("executeEventAction");
        ServerResponse result = target.executeEventAction(requestIndex, lastReceivedRequestIndex, formEvent, pushAsyncResult);
        logRemoteMethodEndCall("executeEventAction", result);
        return result;
    }

    @Override
    public ServerResponse expandGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, int groupId, boolean current) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNodeRecursive");
        ServerResponse result = target.expandGroupObjectRecursive(requestIndex, lastReceivedRequestIndex, groupId, current);
        logRemoteMethodEndVoidCall("expandTreeNodeRecursive");
        return result;
    }

    public ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] treePath) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNode");
        ServerResponse result = target.expandGroupObject(requestIndex, lastReceivedRequestIndex, groupId, treePath);
        logRemoteMethodEndVoidCall("expandTreeNode");
        return result;
    }

    @Override
    public ServerResponse collapseGroupObjectRecursive(long requestIndex, long lastReceivedRequestIndex, int groupId, boolean current) throws RemoteException {
        logRemoteMethodStartVoidCall("collapseTreeNodeRecursive");
        ServerResponse result = target.collapseGroupObjectRecursive(requestIndex, lastReceivedRequestIndex, groupId, current);
        logRemoteMethodEndVoidCall("collapseTreeNodeRecursive");
        return result;
    }

    public ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException {
        logRemoteMethodStartVoidCall("collapseTreeNode");
        ServerResponse result = target.collapseGroupObject(requestIndex, lastReceivedRequestIndex, groupId, bytes);
        logRemoteMethodEndVoidCall("collapseTreeNode");
        return result;
    }

    @Override
    public ServerResponse executeEventAction(long requestIndex, long lastReceivedRequestIndex, String actionSID, int[] propertyIDs, byte[][] fullKeys, EventSource[] eventSources, byte[][] pushAsyncResults) throws RemoteException {
        logRemoteMethodStartCall("executeEventAction");
        ServerResponse result = target.executeEventAction(requestIndex, lastReceivedRequestIndex, actionSID, propertyIDs, fullKeys, eventSources, pushAsyncResults);
        logRemoteMethodEndCall("executeEventAction", result);
        return result;
    }

    @Override
    public ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, String notification) throws RemoteException {
        logRemoteMethodStartCall("executeNotificationAction");
        ServerResponse result = target.executeNotificationAction(requestIndex, lastReceivedRequestIndex, notification);
        logRemoteMethodEndCall("executeNotificationAction", result);
        return result;
    }

    @Override
    public byte[] getAsyncValues(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID, String value, int index, int increaseValuesNeededCount) throws RemoteException {
        return target.getAsyncValues(requestIndex, lastReceivedRequestIndex, propertyID, fullKey, actionSID, value, index, increaseValuesNeededCount);
    }
}
