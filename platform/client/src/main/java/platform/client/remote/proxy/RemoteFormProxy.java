package platform.client.remote.proxy;

import platform.interop.action.ClientApply;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;
import platform.interop.remote.MethodInvocation;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RemoteFormProxy<T extends RemoteFormInterface>
        extends RemoteObjectProxy<T>
        implements RemoteFormInterface {
    private static Logger logger = Logger.getLogger(RemoteFormProxy.class.getName());

    public static final Map<Integer, byte[]> cachedRichDesign = new HashMap<Integer, byte[]>();

    public RemoteFormProxy(T target) {
        super(target);
    }

    public byte[] getReportDesignByteArray(boolean toExcel) throws RemoteException {
        logRemoteMethodStartCall("getReportDesignByteArray");
        byte[] result = target.getReportDesignByteArray(toExcel);
        logRemoteMethodEndCall("getReportDesignByteArray", result);
        return result;
    }

    @ImmutableMethod
    public boolean hasCustomReportDesign() throws RemoteException {
        logRemoteMethodStartCall("hasCustomReportDesign");
        boolean result = target.hasCustomReportDesign();
        logRemoteMethodEndCall("hasCustomReportDesign", result);
        return result;
    }

    public byte[] getReportDataByteArray() throws RemoteException {
        logRemoteMethodStartCall("getReportDataByteArray");
        byte[] result = target.getReportDataByteArray();
        logRemoteMethodEndCall("getReportDataByteArray", result);
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
    public void changePageSize(int groupID, int pageSize) throws RemoteException {
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
    public void changePropertyDraw(int propertyID, byte[] object, boolean all) throws RemoteException {
        logRemoteMethodStartCall("changePropertyDraw");
        target.changePropertyDraw(propertyID, object, all);
        logRemoteMethodEndVoidCall("changePropertyDraw");
    }

    @PendingRemoteMethod
    public void changePropertyDrawWithColumnKeys(int propertyID, byte[] object, boolean all, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartCall("changePropertyDrawWithColumnKeys");
        target.changePropertyDrawWithColumnKeys(propertyID, object, all, columnKeys);
        logRemoteMethodEndVoidCall("changePropertyDrawWithColumnKeys");
    }

    @PendingRemoteMethod
    public void addObject(int objectID, int classID) throws RemoteException {
        logRemoteMethodStartVoidCall("addObject");
        target.addObject(objectID, classID);
        logRemoteMethodEndVoidCall("addObject");
    }

    @PendingRemoteMethod
    public void changeClass(int objectID, int classID) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClass");
        target.changeClass(objectID, classID);
        logRemoteMethodEndVoidCall("changeClass");
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
    public void changeClassView(int groupID, byte classView) throws RemoteException {
        logRemoteMethodStartVoidCall("changeClassView");
        target.changeClassView(groupID, classView);
        logRemoteMethodEndVoidCall("changeClassView");
    }

    @PendingRemoteMethod
    public void changePropertyOrder(int propertyID, byte modiType) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrder");
        target.changePropertyOrder(propertyID, modiType);
        logRemoteMethodEndVoidCall("changePropertyOrder");
    }

    @PendingRemoteMethod
    public void changePropertyOrderWithColumnKeys(int propertyID, byte modiType, byte[] columnKeys) throws RemoteException {
        logRemoteMethodStartVoidCall("changePropertyOrderWithColumnKeys");
        target.changePropertyOrderWithColumnKeys(propertyID, modiType, columnKeys);
        logRemoteMethodEndVoidCall("changePropertyOrderWithColumnKeys");
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

    public ClientApply getClientApply() throws RemoteException {
        logRemoteMethodStartCall("getClientApply");
        return target.getClientApply();
    }

    @PendingRemoteMethod
    public void applyClientChanges(Object clientResult) throws RemoteException {
        logRemoteMethodStartVoidCall("applyClientChanges");
        target.applyClientChanges(clientResult);
        logRemoteMethodEndVoidCall("applyClientChanges");
    }

    @PendingRemoteMethod
    public void applyChanges() throws RemoteException {
        logRemoteMethodStartVoidCall("applyChanges");
        target.applyChanges();
        logRemoteMethodStartVoidCall("applyChanges");
    }

    @PendingRemoteMethod
    public void cancelChanges() throws RemoteException {
        logRemoteMethodStartVoidCall("cancelChanges");
        target.cancelChanges();
        logRemoteMethodEndVoidCall("cancelChanges");
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
    public RemoteDialogInterface createObjectDialog(int objectID) throws RemoteException {
        return createDialog("createObjectDialog", objectID);
    }

    @NonPendingRemoteMethod
    public RemoteDialogInterface createObjectDialogWithValue(int objectID, int value) throws RemoteException {
        return createDialog("createObjectDialogWithValue", objectID, value);
    }
}
