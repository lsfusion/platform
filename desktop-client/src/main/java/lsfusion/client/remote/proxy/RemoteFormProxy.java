package lsfusion.client.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.interop.ClassViewType;
import lsfusion.interop.form.FormUserPreferences;
import lsfusion.interop.form.RemoteFormInterface;
import lsfusion.interop.form.ReportGenerationData;
import lsfusion.interop.form.ServerResponse;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class RemoteFormProxy<T extends RemoteFormInterface>
        extends RemoteObjectProxy<T>
        implements RemoteFormInterface {

    public static final Map<String, byte[]> cachedRichDesign = new HashMap<String, byte[]>();
    public static void dropCaches() {
        cachedRichDesign.clear();
    }

    public RemoteFormProxy(T target) {
        super(target);
    }

    @ImmutableMethod
    public String getSID() throws RemoteException {
        try {
            return callImmutableMethod("getSID", new Callable<String>() {
                @Override
                public String call() throws Exception {
                    logRemoteMethodStartCall("getSID");
                    String result = target.getSID();
                    logRemoteMethodEndCall("getSID", result);
                    return result;
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
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

    public ReportGenerationData getReportData(long requestIndex, Integer groupId, boolean toExcel, FormUserPreferences userPreferences) throws RemoteException {
        return target.getReportData(requestIndex, groupId, toExcel, userPreferences);
    }

    public Map<String, String> getReportPath(long requestIndex, boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException {
        return target.getReportPath(requestIndex, toExcel, groupId, userPreferences);
    }

    public ServerResponse getRemoteChanges(long requestIndex) throws RemoteException {
        logRemoteMethodStartCall("getRemoteChanges");
        ServerResponse result = target.getRemoteChanges(requestIndex);
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    public ServerResponse changePageSize(long requestIndex, int groupID, Integer pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("changePageSize");
        ServerResponse result = target.changePageSize(requestIndex, groupID, pageSize);
        logRemoteMethodEndVoidCall("changePageSize");
        return result;
    }

    public void gainedFocus(long requestIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("gainedFocus");
        target.gainedFocus(requestIndex);
        logRemoteMethodEndVoidCall("gainedFocus");
    }

    public ServerResponse setTabVisible(long requestIndex, int tabPaneID, int tabIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("setTabVisible");
        ServerResponse result = target.setTabVisible(requestIndex, tabPaneID, tabIndex);
        logRemoteMethodEndVoidCall("setTabVisible");
        return result;
    }

    public ServerResponse changeGroupObject(long requestIndex, int groupID, byte[] value) throws RemoteException {
        logRemoteMethodStartCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(requestIndex, groupID, value);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    public ServerResponse changeGroupObject(long requestIndex, int groupID, byte changeType) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(requestIndex, groupID, changeType);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    public ServerResponse pasteExternalTable(long requestIndex, List<Integer> propertyIDs, List<byte[]> columnKeys, List<List<byte[]>> values) throws RemoteException {
        logRemoteMethodStartCall("pasteExternalTable");
        ServerResponse result = target.pasteExternalTable(requestIndex, propertyIDs, columnKeys, values);
        logRemoteMethodEndCall("pasteExternalTable", result);
        return result;
    }

    public ServerResponse pasteMulticellValue(long requestIndex, Map<Integer, List<byte[]>> keys, Map<Integer, byte[]> values) throws RemoteException {
        logRemoteMethodStartCall("pasteMulticellValue");
        ServerResponse result = target.pasteMulticellValue(requestIndex, keys, values);
        logRemoteMethodEndCall("pasteMulticellValue", result);
        return result;
    }

    public ServerResponse changeGridClass(long requestIndex, int objectID, int idClass) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGridClass");
        ServerResponse result = target.changeGridClass(requestIndex, objectID, idClass);
        logRemoteMethodEndVoidCall("changeGridClass");
        return result;
    }

    public ServerResponse changeClassView(long requestIndex, int groupID, ClassViewType classView) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClassView");
        ServerResponse result = target.changeClassView(requestIndex, groupID, classView);
        logRemoteMethodEndVoidCall("changeClassView");
        return result;
    }

    public ServerResponse changePropertyOrder(long requestIndex, int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        ServerResponse result = target.changePropertyOrder(requestIndex, propertyID, modiType, columnKeys);
        logRemoteMethodEndCall("changePropertyOrder", result);
        return result;
    }

    @Override
    public ServerResponse clearPropertyOrders(long requestIndex, int groupObjectID) throws RemoteException {
        logRemoteMethodStartVoidCall("clearPropertyOrders");
        ServerResponse result = target.clearPropertyOrders(requestIndex, groupObjectID);
        logRemoteMethodEndCall("clearPropertyOrders", result);
        return result;
    }

    public ServerResponse setUserFilters(long requestIndex, byte[][] filters) throws RemoteException {
        logRemoteMethodStartVoidCall("setUserFilters");
        ServerResponse result = target.setUserFilters(requestIndex, filters);
        logRemoteMethodEndCall("setUserFilters", result);
        return result;
    }

    public ServerResponse setRegularFilter(long requestIndex, int groupID, int filterID) throws RemoteException {
        logRemoteMethodStartVoidCall("setRegularFilter");
        ServerResponse result = target.setRegularFilter(requestIndex, groupID, filterID);
        logRemoteMethodEndCall("setRegularFilter", result);
        return result;
    }

    public int countRecords(long requestIndex, int groupObjectID) throws RemoteException {
        logRemoteMethodStartCall("countRecords");
        int result = target.countRecords(requestIndex, groupObjectID);
        logRemoteMethodEndCall("countRecords", result);
        return result;
    }

    public Object calculateSum(long requestIndex, int propertyID, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartCall("calculateSum");
        Object result = target.calculateSum(requestIndex, propertyID, columnKeys);
        logRemoteMethodEndCall("calculateSum", result);
        return result;
    }

    public Map<List<Object>, List<Object>> groupData(long requestIndex, Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                                     Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException {
        logRemoteMethodStartCall("groupData");
        Map<List<Object>, List<Object>> result = target.groupData(requestIndex, groupMap, sumMap, maxMap, onlyNotNull);
        logRemoteMethodEndCall("groupData", result);
        return result;
    }

    public void saveUserPreferences(long requestIndex, FormUserPreferences preferences, boolean forAllUsers) throws RemoteException {
        target.saveUserPreferences(requestIndex, preferences, forAllUsers);
    }

    public ServerResponse okPressed(long requestIndex) throws RemoteException {
        logRemoteMethodStartCall("okPressed");
        ServerResponse result = target.okPressed(requestIndex);
        logRemoteMethodEndCall("okPressed", result);
        return result;
    }

    public ServerResponse closedPressed(long requestIndex) throws RemoteException {
        logRemoteMethodStartCall("closedPressed");
        ServerResponse result = target.closedPressed(requestIndex);
        logRemoteMethodEndCall("closedPressed", result);
        return result;
    }

    public ServerResponse expandGroupObject(long requestIndex, int groupId, byte[] treePath) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNode");
        ServerResponse result = target.expandGroupObject(requestIndex, groupId, treePath);
        logRemoteMethodEndVoidCall("expandTreeNode");
        return result;
    }

    public ServerResponse collapseGroupObject(long requestIndex, int groupId, byte[] bytes) throws RemoteException {
        logRemoteMethodStartVoidCall("collapseTreeNode");
        ServerResponse result = target.collapseGroupObject(requestIndex, groupId, bytes);
        logRemoteMethodEndVoidCall("collapseTreeNode");
        return result;
    }

    public ServerResponse moveGroupObject(long requestIndex, int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        logRemoteMethodStartVoidCall("moveGroupObject");
        ServerResponse result = target.moveGroupObject(requestIndex, parentGroupId, parentKey, childGroupId, childKey, index);
        logRemoteMethodEndVoidCall("moveGroupObject");
        return result;
    }

    public ServerResponse executeEditAction(long requestIndex, int propertyID, byte[] columnKey, String actionSID) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.executeEditAction(requestIndex, propertyID, columnKey, actionSID);
        logRemoteMethodEndCall("getPropertyChangeType", result);
        return result;
    }

    public ServerResponse changeProperty(long requestIndex, int propertyID, byte[] fullKey, byte[] pushChange, Integer pushAdd) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.changeProperty(requestIndex, propertyID, fullKey, pushChange, pushAdd);
        logRemoteMethodEndCall("executeEditAction", result);
        return result;
    }

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        logRemoteMethodStartCall("continueServerInvocation");
        ServerResponse result = target.continueServerInvocation(actionResults);
        logRemoteMethodEndCall("continueServerInvocation", result);
        return result;
    }

    public ServerResponse throwInServerInvocation(Throwable clientThrowable) throws RemoteException {
        logRemoteMethodStartCall("throwInServerInvocation");
        ServerResponse result = target.throwInServerInvocation(clientThrowable);
        logRemoteMethodEndCall("throwInServerInvocation", result);
        return result;
    }
}
