package lsfusion.client.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.FormGrouping;
import lsfusion.interop.form.*;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RemoteFormProxy extends RemoteObjectProxy<RemoteFormInterface> implements RemoteFormInterface {

    public static final Map<String, byte[]> cachedRichDesign = new HashMap<>();
    public static void dropCaches() {
        cachedRichDesign.clear();
    }

    public RemoteFormProxy(RemoteFormInterface target) {
        super(target);
    }

    @ImmutableMethod
    public FormUserPreferences getUserPreferences() throws RemoteException {
        try {
            return callImmutableMethod("getUserPreferences", new Callable<FormUserPreferences>() {
                @Override
                public FormUserPreferences call() throws Exception {
                    return target.getUserPreferences();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public ColorPreferences getColorPreferences() throws RemoteException {
        try {
            return callImmutableMethod("getColorPreferences", new Callable<ColorPreferences>() {
                @Override
                public ColorPreferences call() throws Exception {
                    return target.getColorPreferences();
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public byte[] getRichDesignByteArray() throws RemoteException {
        try {
            return callImmutableMethod("getRichDesignByteArray", new Callable<byte[]>() {
                @Override
                public byte[] call() throws Exception {
                    logRemoteMethodStartCall("getRichDesignByteArray");
                    byte[] result = target.getRichDesignByteArray();
                    logRemoteMethodEndCall("getRichDesignByteArray", result);
                    return result;
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    @ImmutableMethod
    public Integer getInitFilterPropertyDraw() throws RemoteException {
        try {
            return callImmutableMethod("getInitFilterPropertyDraw", new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    logRemoteMethodStartCall("getInitFilterPropertyDraw");
                    Integer result = target.getInitFilterPropertyDraw();
                    logRemoteMethodEndCall("getInitFilterPropertyDraw", result);
                    return result;
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public ReportGenerationData getReportData(long requestIndex, long lastReceivedRequestIndex, Integer groupId, boolean toExcel, FormUserPreferences userPreferences) throws RemoteException {
        return target.getReportData(requestIndex, lastReceivedRequestIndex, groupId, toExcel, userPreferences);
    }

    public Map<String, String> getReportPath(long requestIndex, long lastReceivedRequestIndex, boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException {
        return target.getReportPath(requestIndex, lastReceivedRequestIndex, toExcel, groupId, userPreferences);
    }

    public ServerResponse getRemoteChanges(long requestIndex, long lastReceivedRequestIndex, boolean refresh) throws RemoteException {
        logRemoteMethodStartCall("getRemoteChanges");
        ServerResponse result = target.getRemoteChanges(requestIndex, lastReceivedRequestIndex, refresh);
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    public ServerResponse changePageSize(long requestIndex, long lastReceivedRequestIndex, int groupID, Integer pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("changePageSize");
        ServerResponse result = target.changePageSize(requestIndex, lastReceivedRequestIndex, groupID, pageSize);
        logRemoteMethodEndVoidCall("changePageSize");
        return result;
    }

    public void gainedFocus(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("gainedFocus");
        target.gainedFocus(requestIndex, lastReceivedRequestIndex);
        logRemoteMethodEndVoidCall("gainedFocus");
    }

    public ServerResponse setTabVisible(long requestIndex, long lastReceivedRequestIndex, int tabPaneID, int childId) throws RemoteException {
        logRemoteMethodStartVoidCall("setTabVisible");
        ServerResponse result = target.setTabVisible(requestIndex, lastReceivedRequestIndex, tabPaneID, childId);
        logRemoteMethodEndVoidCall("setTabVisible");
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

    public ServerResponse pasteExternalTable(long requestIndex, long lastReceivedRequestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values) throws RemoteException {
        logRemoteMethodStartCall("pasteExternalTable");
        ServerResponse result = target.pasteExternalTable(requestIndex, lastReceivedRequestIndex, propertyIDs, columnKeys, values);
        logRemoteMethodEndCall("pasteExternalTable", result);
        return result;
    }

    public ServerResponse pasteMulticellValue(long requestIndex, long lastReceivedRequestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values) throws RemoteException {
        logRemoteMethodStartCall("pasteMulticellValue");
        ServerResponse result = target.pasteMulticellValue(requestIndex, lastReceivedRequestIndex, keys, values);
        logRemoteMethodEndCall("pasteMulticellValue", result);
        return result;
    }

    public ServerResponse changeGridClass(long requestIndex, long lastReceivedRequestIndex, int objectID, int idClass) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGridClass");
        ServerResponse result = target.changeGridClass(requestIndex, lastReceivedRequestIndex, objectID, idClass);
        logRemoteMethodEndVoidCall("changeGridClass");
        return result;
    }

    public ServerResponse changeClassView(long requestIndex, long lastReceivedRequestIndex, int groupID, ClassViewType classView) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClassView");
        ServerResponse result = target.changeClassView(requestIndex, lastReceivedRequestIndex, groupID, classView);
        logRemoteMethodEndVoidCall("changeClassView");
        return result;
    }

    public ServerResponse changePropertyOrder(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        ServerResponse result = target.changePropertyOrder(requestIndex, lastReceivedRequestIndex, propertyID, modiType, columnKeys);
        logRemoteMethodEndCall("changePropertyOrder", result);
        return result;
    }

    @Override
    public ServerResponse clearPropertyOrders(long requestIndex, long lastReceivedRequestIndex, int groupObjectID) throws RemoteException {
        logRemoteMethodStartVoidCall("clearPropertyOrders");
        ServerResponse result = target.clearPropertyOrders(requestIndex, lastReceivedRequestIndex, groupObjectID);
        logRemoteMethodEndCall("clearPropertyOrders", result);
        return result;
    }

    public ServerResponse setUserFilters(long requestIndex, long lastReceivedRequestIndex, byte[][] filters) throws RemoteException {
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

    public ServerResponse okPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        logRemoteMethodStartCall("okPressed");
        ServerResponse result = target.okPressed(requestIndex, lastReceivedRequestIndex);
        logRemoteMethodEndCall("okPressed", result);
        return result;
    }

    public ServerResponse closedPressed(long requestIndex, long lastReceivedRequestIndex) throws RemoteException {
        logRemoteMethodStartCall("closedPressed");
        ServerResponse result = target.closedPressed(requestIndex, lastReceivedRequestIndex);
        logRemoteMethodEndCall("closedPressed", result);
        return result;
    }

    public ServerResponse expandGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] treePath) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNode");
        ServerResponse result = target.expandGroupObject(requestIndex, lastReceivedRequestIndex, groupId, treePath);
        logRemoteMethodEndVoidCall("expandTreeNode");
        return result;
    }

    public ServerResponse collapseGroupObject(long requestIndex, long lastReceivedRequestIndex, int groupId, byte[] bytes) throws RemoteException {
        logRemoteMethodStartVoidCall("collapseTreeNode");
        ServerResponse result = target.collapseGroupObject(requestIndex, lastReceivedRequestIndex, groupId, bytes);
        logRemoteMethodEndVoidCall("collapseTreeNode");
        return result;
    }

    public ServerResponse moveGroupObject(long requestIndex, long lastReceivedRequestIndex, int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        logRemoteMethodStartVoidCall("moveGroupObject");
        ServerResponse result = target.moveGroupObject(requestIndex, lastReceivedRequestIndex, parentGroupId, parentKey, childGroupId, childKey, index);
        logRemoteMethodEndVoidCall("moveGroupObject");
        return result;
    }

    public ServerResponse executeEditAction(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, String actionSID) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.executeEditAction(requestIndex, lastReceivedRequestIndex, propertyID, fullKey, actionSID);
        logRemoteMethodEndCall("executeEditAction", result);
        return result;
    }

    @Override
    public ServerResponse executeNotificationAction(long requestIndex, long lastReceivedRequestIndex, int idNotification) throws RemoteException {
        logRemoteMethodStartCall("executeNotificationAction");
        ServerResponse result = target.executeNotificationAction(requestIndex, lastReceivedRequestIndex, idNotification);
        logRemoteMethodEndCall("executeNotificationAction", result);
        return result;
    }

    public ServerResponse changeProperty(long requestIndex, long lastReceivedRequestIndex, int propertyID, byte[] fullKey, byte[] pushChange, Integer pushAdd) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.changeProperty(requestIndex, lastReceivedRequestIndex, propertyID, fullKey, pushChange, pushAdd);
        logRemoteMethodEndCall("executeEditAction", result);
        return result;
    }

    public ServerResponse continueServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Object[] actionResults) throws RemoteException {
        logRemoteMethodStartCall("continueServerInvocation");
        ServerResponse result = target.continueServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, actionResults);
        logRemoteMethodEndCall("continueServerInvocation", result);
        return result;
    }

    public ServerResponse throwInServerInvocation(long requestIndex, long lastReceivedRequestIndex, int continueIndex, Throwable clientThrowable) throws RemoteException {
        logRemoteMethodStartCall("throwInServerInvocation");
        ServerResponse result = target.throwInServerInvocation(requestIndex, lastReceivedRequestIndex, continueIndex, clientThrowable);
        logRemoteMethodEndCall("throwInServerInvocation", result);
        return result;
    }

    @Override
    public boolean isInServerInvocation(long requestIndex) throws RemoteException {
        logRemoteMethodStartCall("isInServerInvocation");
        boolean result = target.isInServerInvocation(requestIndex);
        logRemoteMethodEndCall("isInServerInvocation", result);
        return result;
    }
}
