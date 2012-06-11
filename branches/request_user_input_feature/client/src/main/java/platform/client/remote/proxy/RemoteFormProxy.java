package platform.client.remote.proxy;

import platform.interop.ClassViewType;
import platform.interop.form.FormUserPreferences;
import platform.interop.form.RemoteFormInterface;
import platform.interop.form.ServerResponse;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public byte[] getReportDesignsByteArray(boolean toExcel, FormUserPreferences userPreferences) throws RemoteException {
        logRemoteMethodStartCall("getReportDesignsByteArray");
        byte[] result = target.getReportDesignsByteArray(toExcel, userPreferences);
        logRemoteMethodEndCall("getReportDesignsByteArray", result);
        return result;
    }

    public byte[] getSingleGroupReportDesignByteArray(boolean toExcel, int groupId, FormUserPreferences userPreferences) throws RemoteException {
        logRemoteMethodStartCall("getSingleGroupReportDesignByteArray");
        byte[] result = target.getSingleGroupReportDesignByteArray(toExcel, groupId, userPreferences);
        logRemoteMethodEndCall("getSingleGroupReportDesignByteArray", result);
        return result;
    }

    public byte[] getReportSourcesByteArray() throws RemoteException {
        logRemoteMethodStartCall("getReportSourcesByteArray");
        byte[] result = target.getReportSourcesByteArray();
        logRemoteMethodEndCall("getReportSourcesByteArray", result);
        return result;
    }

    public byte[] getSingleGroupReportSourcesByteArray(int groupId) throws RemoteException {
        logRemoteMethodStartCall("getSingleGroupReportSourcesByteArray");
        byte[] result = target.getSingleGroupReportSourcesByteArray(groupId);
        logRemoteMethodEndCall("getSingleGroupReportSourcesByteArray", result);
        return result;
    }

    public Map<String, String> getReportPath(boolean toExcel, Integer groupId, FormUserPreferences userPreferences) throws RemoteException {
        return target.getReportPath(toExcel, groupId, userPreferences);
    }

    public byte[] getReportHierarchyByteArray() throws RemoteException {
        logRemoteMethodStartCall("getReportHierarchyByteArray");
        byte[] result = target.getReportHierarchyByteArray();
        logRemoteMethodEndCall("getReportHierarchyByteArray", result);
        return result;
    }

    public byte[] getSingleGroupReportHierarchyByteArray(int groupId) throws RemoteException {
        logRemoteMethodStartCall("getSingleGroupReportHierarchyByteArray");
        byte[] result = target.getSingleGroupReportHierarchyByteArray(groupId);
        logRemoteMethodEndCall("getSingleGroupReportHierarchyByteArray", result);
        return result;
    }

    public ServerResponse getRemoteChanges() throws RemoteException {
        logRemoteMethodStartCall("getRemoteChanges");
        ServerResponse result = target.getRemoteChanges();
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    @ImmutableMethod
    public byte[] getRichDesignByteArray() throws RemoteException {
        logRemoteMethodStartCall("getRichDesignByteArray");
        byte[] result = target.getRichDesignByteArray();
        logRemoteMethodEndCall("getRichDesignByteArray", result);
        return result;
    }

    public ServerResponse changePageSize(int groupID, Integer pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("changePageSize");
        ServerResponse result = target.changePageSize(groupID, pageSize);
        logRemoteMethodEndVoidCall("changePageSize");
        return result;
    }

    @PendingRemoteMethod
    public void gainedFocus() throws RemoteException {
        logRemoteMethodStartVoidCall("gainedFocus");
        target.gainedFocus();
        logRemoteMethodEndVoidCall("gainedFocus");
    }

    public ServerResponse setTabVisible(int tabPaneID, int tabIndex) throws RemoteException {
        logRemoteMethodStartVoidCall("setTabVisible");
        ServerResponse result = target.setTabVisible(tabPaneID, tabIndex);
        logRemoteMethodEndVoidCall("setTabVisible");
        return result;
    }

    public ServerResponse changeGroupObject(int groupID, byte[] value) throws RemoteException {
        logRemoteMethodStartCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(groupID, value);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    public ServerResponse changeGroupObject(int groupID, byte changeType) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGroupObject");
        ServerResponse result = target.changeGroupObject(groupID, changeType);
        logRemoteMethodEndVoidCall("changeGroupObject");
        return result;
    }

    public ServerResponse pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws RemoteException {
        logRemoteMethodStartCall("pasteExternalTable");
        ServerResponse result = target.pasteExternalTable(propertyIDs, table);
        logRemoteMethodEndCall("pasteExternalTable", result);
        return result;
    }

    public ServerResponse pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws RemoteException {
        logRemoteMethodStartCall("pasteMulticellValue");
        ServerResponse result = target.pasteMulticellValue(cells, value);
        logRemoteMethodEndCall("pasteMulticellValue", result);
        return result;
    }

    public ServerResponse changeGridClass(int objectID, int idClass) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGridClass");
        ServerResponse result = target.changeGridClass(objectID, idClass);
        logRemoteMethodEndVoidCall("changeGridClass");
        return result;
    }

    public ServerResponse changeClassView(int groupID, ClassViewType classView) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClassView");
        ServerResponse result = target.changeClassView(groupID, classView);
        logRemoteMethodEndVoidCall("changeClassView");
        return result;
    }

    public ServerResponse changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        ServerResponse result = target.changePropertyOrder(propertyID, modiType, columnKeys);
        logRemoteMethodEndCall("changePropertyOrder", result);
        return result;
    }

    public ServerResponse setUserFilters(byte[][] filters) throws RemoteException {
        logRemoteMethodStartVoidCall("setUserFilters");
        ServerResponse result = target.setUserFilters(filters);
        logRemoteMethodEndCall("setUserFilters", result);
        return result;
    }

    public ServerResponse setRegularFilter(int groupID, int filterID) throws RemoteException {
        logRemoteMethodStartVoidCall("setRegularFilter");
        ServerResponse result = target.setRegularFilter(groupID, filterID);
        logRemoteMethodEndCall("setRegularFilter", result);
        return result;
    }

    public int countRecords(int groupObjectID) throws RemoteException {
        logRemoteMethodStartCall("countRecords");
        int result = target.countRecords(groupObjectID);
        logRemoteMethodEndCall("countRecords", result);
        return result;
    }

    public Object calculateSum(int propertyID, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartCall("calculateSum");
        Object result = target.calculateSum(propertyID, columnKeys);
        logRemoteMethodEndCall("calculateSum", result);
        return result;
    }

    public Map<List<Object>, List<Object>> groupData(Map<Integer, List<byte[]>> groupMap, Map<Integer, List<byte[]>> sumMap,
                                                     Map<Integer, List<byte[]>> maxMap, boolean onlyNotNull) throws RemoteException {
        logRemoteMethodStartCall("groupData");
        Map<List<Object>, List<Object>> result = target.groupData(groupMap, sumMap, maxMap, onlyNotNull);
        logRemoteMethodEndCall("groupData", result);
        return result;
    }

    @ImmutableMethod
    public String getSID() throws RemoteException {
        logRemoteMethodStartCall("getSID");
        String result = target.getSID();
        logRemoteMethodEndCall("getSID", result);
        return result;
    }

    public void saveUserPreferences(FormUserPreferences preferences, Boolean forAllUsers) throws RemoteException {
        target.saveUserPreferences(preferences, forAllUsers);
    }

    @ImmutableMethod
    public FormUserPreferences loadUserPreferences() throws RemoteException {
        return target.loadUserPreferences();
    }

    public ServerResponse okPressed() throws RemoteException {
        logRemoteMethodStartCall("okPressed");
        ServerResponse result = target.okPressed();
        logRemoteMethodEndCall("okPressed", result);
        return result;
    }

    public ServerResponse closedPressed() throws RemoteException {
        logRemoteMethodStartCall("closedPressed");
        ServerResponse result = target.closedPressed();
        logRemoteMethodEndCall("closedPressed", result);
        return result;
    }

    public ServerResponse expandGroupObject(int groupId, byte[] treePath) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNode");
        ServerResponse result = target.expandGroupObject(groupId, treePath);
        logRemoteMethodEndVoidCall("expandTreeNode");
        return result;
    }

    public ServerResponse collapseGroupObject(int groupId, byte[] bytes) throws RemoteException {
        logRemoteMethodStartVoidCall("collapseTreeNode");
        ServerResponse result = target.collapseGroupObject(groupId, bytes);
        logRemoteMethodEndVoidCall("collapseTreeNode");
        return result;
    }

    public ServerResponse moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        logRemoteMethodStartVoidCall("moveGroupObject");
        ServerResponse result = target.moveGroupObject(parentGroupId, parentKey, childGroupId, childKey, index);
        logRemoteMethodEndVoidCall("moveGroupObject");
        return result;
    }

    public ServerResponse executeEditAction(int propertyID, byte[] columnKey, String actionSID) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.executeEditAction(propertyID, columnKey, actionSID);
        logRemoteMethodEndCall("getPropertyChangeType", result);
        return result;
    }

    public ServerResponse changeProperty(int propertyID, byte[] fullKey, byte[] value) throws RemoteException {
        logRemoteMethodStartCall("executeEditAction");
        ServerResponse result = target.changeProperty(propertyID, fullKey, value);
        logRemoteMethodEndCall("getPropertyChangeType", result);
        return result;
    }

    public ServerResponse continueServerInvocation(Object[] actionResults) throws RemoteException {
        logRemoteMethodStartCall("continueServerInvocation");
        ServerResponse result = target.continueServerInvocation(actionResults);
        logRemoteMethodEndCall("continueServerInvocation", result);
        return result;
    }

    public ServerResponse throwInServerInvocation(Exception clientException) throws RemoteException {
        logRemoteMethodStartCall("throwInServerInvocation");
        ServerResponse result = target.throwInServerInvocation(clientException);
        logRemoteMethodEndCall("throwInServerInvocation", result);
        return result;
    }

    public String getRemoteActionMessage() throws RemoteException {
        return target.getRemoteActionMessage();
    }
}
