package platform.client.remote.proxy;

import platform.interop.ClassViewType;
import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RemoteFormProxy<T extends RemoteFormInterface>
        extends RemoteObjectProxy<T>
        implements RemoteFormInterface {

    public static final Map<Integer, byte[]> cachedRichDesign = new HashMap<Integer, byte[]>();

    public RemoteFormProxy(T target) {
        super(target);
    }
    
    @ImmutableMethod
    public boolean hasCustomReportDesign() throws RemoteException {
        logRemoteMethodStartCall("hasCustomReportDesign");
        boolean result = target.hasCustomReportDesign();
        logRemoteMethodEndCall("hasCustomReportDesign", result);
        return result;
    }

    public byte[] getReportDesignsByteArray(boolean toExcel) throws RemoteException {
        logRemoteMethodStartCall("getReportDesignsByteArray");
        byte[] result = target.getReportDesignsByteArray(toExcel);
        logRemoteMethodEndCall("getReportDesignsByteArray", result);
        return result;
    }

    public byte[] getReportSourcesByteArray() throws RemoteException {
        logRemoteMethodStartCall("getReportSourcesByteArray");
        byte[] result = target.getReportSourcesByteArray();
        logRemoteMethodEndCall("getReportSourcesByteArray", result);
        return result;
    }

    public byte[] getReportHierarchyByteArray() throws RemoteException {
        logRemoteMethodStartCall("getReportHierarchyByteArray");
        byte[] result = target.getReportHierarchyByteArray();
        logRemoteMethodEndCall("getReportHierarchyByteArray", result);
        return result;
    }
    
    public RemoteChanges getRemoteChanges() throws RemoteException {
        logRemoteMethodStartCall("getRemoteChanges");
        RemoteChanges result = target.getRemoteChanges();
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    @ImmutableMethod
    public byte[] getRichDesignByteArray() throws RemoteException {
        logRemoteMethodStartCall("getRichDesignByteArray");
        byte[] result = target.getRichDesignByteArray();
        logRemoteMethodEndCall("getRemoteChanges", result);
        return result;
    }

    @PendingRemoteMethod
    public void changePageSize(int groupID, Integer pageSize) throws RemoteException {
        logRemoteMethodStartVoidCall("changePageSize");
        target.changePageSize(groupID, pageSize);
        logRemoteMethodEndVoidCall("changePageSize");
    }

    @PendingRemoteMethod
    public void gainedFocus() throws RemoteException {
        logRemoteMethodStartVoidCall("gainedFocus");
        target.gainedFocus();
        logRemoteMethodEndVoidCall("gainedFocus");
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte[] value) throws RemoteException {
        logRemoteMethodStartCall("changeGroupObject");
        target.changeGroupObject(groupID, value);
        logRemoteMethodEndVoidCall("changeGroupObject");
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte changeType) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGroupObject");
        target.changeGroupObject(groupID, changeType);
        logRemoteMethodEndVoidCall("changeGroupObject");
    }

    @PendingRemoteMethod
    public void changePropertyDraw(int propertyID, byte[] object, boolean all, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartCall("changePropertyDraw");
        target.changePropertyDraw(propertyID, object, all, columnKeys);
        logRemoteMethodEndVoidCall("changePropertyDraw");
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        logRemoteMethodStartCall("canChangeClass");
        boolean result = target.canChangeClass(objectID);
        logRemoteMethodEndCall("canChangeClass", result);
        return result;
    }

    @PendingRemoteMethod
    public void changeGridClass(int objectID, int idClass) throws RemoteException {
        logRemoteMethodStartVoidCall("changeGridClass");
        target.changeGridClass(objectID, idClass);
        logRemoteMethodEndVoidCall("changeGridClass");
    }

    @PendingRemoteMethod
    public void switchClassView(int groupID) throws RemoteException {
        logRemoteMethodStartVoidCall("switchClassView");
        target.switchClassView(groupID);
        logRemoteMethodEndVoidCall("switchClassView");
    }

    @PendingRemoteMethod
    public void changeClassView(int groupID, ClassViewType classView) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClassView");
        target.changeClassView(groupID, classView);
        logRemoteMethodEndVoidCall("changeClassView");
    }

    @PendingRemoteMethod
    public void changePropertyOrder(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        target.changePropertyOrder(propertyID, modiType, columnKeys);
        logRemoteMethodEndVoidCall("changePropertyOrder");
    }

    @PendingRemoteMethod
    public void clearUserFilters() throws RemoteException {
        logRemoteMethodStartVoidCall("clearUserFilters");
        target.clearUserFilters();
        logRemoteMethodEndVoidCall("clearUserFilters");
    }

    @PendingRemoteMethod
    public void addFilter(byte[] state) throws RemoteException {
        logRemoteMethodStartVoidCall("addFilter");
        target.addFilter(state);
        logRemoteMethodEndVoidCall("addFilter");
    }

    @PendingRemoteMethod
    public void setRegularFilter(int groupID, int filterID) throws RemoteException {
        logRemoteMethodStartVoidCall("setRegularFilter");
        target.setRegularFilter(groupID, filterID);
        logRemoteMethodEndVoidCall("setRegularFilter");
    }

    @ImmutableMethod
    public int getID() throws RemoteException {
        logRemoteMethodStartCall("getID");
        int result = target.getID();
        logRemoteMethodEndCall("getID", result);
        return result;
    }

    @PendingRemoteMethod
    public void refreshData() throws RemoteException {
        logRemoteMethodStartCall("refreshData");
        target.refreshData();
    }

    @ImmutableMethod
    public boolean hasClientApply() throws RemoteException {
        logRemoteMethodStartCall("hasClientApply");
        return target.hasClientApply();
    }

    public ClientApply checkClientChanges() throws RemoteException {
        logRemoteMethodStartCall("applyClientChanges");
        return target.checkClientChanges();
    }

    @PendingRemoteMethod
    public void applyClientChanges(Object clientResult) throws RemoteException {
        logRemoteMethodStartVoidCall("confirmClientChanges");
        target.applyClientChanges(clientResult);
        logRemoteMethodEndVoidCall("confirmClientChanges");
    }

    @PendingRemoteMethod
    public void applyChanges() throws RemoteException {
        logRemoteMethodStartVoidCall("applyChanges");
        target.applyChanges();
        logRemoteMethodEndVoidCall("applyChanges");
    }

    @PendingRemoteMethod
    public void cancelChanges() throws RemoteException {
        logRemoteMethodStartVoidCall("cancelChanges");
        target.cancelChanges();
        logRemoteMethodEndVoidCall("cancelChanges");
    }

    @PendingRemoteMethod
    public void expandGroupObject(int groupId, byte[] treePath) throws RemoteException {
        logRemoteMethodStartVoidCall("expandTreeNode");
        target.expandGroupObject(groupId, treePath);
        logRemoteMethodEndVoidCall("expandTreeNode");
    }

    @PendingRemoteMethod
    public void moveGroupObject(int parentGroupId, byte[] parentKey, int childGroupId, byte[] childKey, int index) throws RemoteException {
        logRemoteMethodStartVoidCall("moveGroupObject");
        target.moveGroupObject(parentGroupId, parentKey, childGroupId, childKey, index);
        logRemoteMethodEndVoidCall("moveGroupObject");
    }

    public byte[] getPropertyChangeType(int propertyID) throws RemoteException {
        logRemoteMethodStartCall("getPropertyChangeType");
        byte[] result = target.getPropertyChangeType(propertyID);
        logRemoteMethodEndCall("getPropertyChangeType", result);
        return result;
    }

    @NonFlushRemoteMethod
    private RemoteDialogInterface createDialog(String methodName, Object... args) throws RemoteException {
        List<MethodInvocation> invocations = getImmutableMethodInvocations(RemoteDialogProxy.class);

        MethodInvocation creator = MethodInvocation.create(this.getClass(), methodName, args);

        Object[] result = createAndExecute(creator, invocations.toArray(new MethodInvocation[invocations.size()]));

        RemoteDialogInterface remoteDialog = (RemoteDialogInterface) result[0];
        if (remoteDialog == null) {
            return null;
        }

        RemoteDialogProxy proxy = new RemoteDialogProxy(remoteDialog);
        for (int i = 0; i < invocations.size(); ++i) {
            proxy.setProperty(invocations.get(i).name, result[i + 1]);
        }

        return proxy;
    }

    @NonPendingRemoteMethod
    public RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException {
        return createDialog("createClassPropertyDialog", viewID, value);
    }

    @NonPendingRemoteMethod
    public RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException {
        return createDialog("createEditorPropertyDialog", viewID);
    }

    @NonPendingRemoteMethod
    public RemoteDialogInterface createObjectEditorDialog(int viewID) throws RemoteException {
        return createDialog("createObjectEditorDialog", viewID);
    }
}
