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
        logRemoteMethodCall("getReportDesignByteArray");
        return target.getReportDesignByteArray(toExcel);
    }

    @ImmutableMethod
    public boolean hasCustomReportDesign() throws RemoteException {
        logRemoteMethodCall("hasCustomReportDesign");
        return target.hasCustomReportDesign();
    }

    public byte[] getReportDataByteArray() throws RemoteException {
        logRemoteMethodCall("getReportDataByteArray");
        return target.getReportDataByteArray();
    }

    public RemoteChanges getRemoteChanges() throws RemoteException {
        logRemoteMethodCall("getRemoteChanges");
        return target.getRemoteChanges();
    }

    @ImmutableMethod
    public byte[] getRichDesignByteArray() throws RemoteException {
        logRemoteMethodCall("getRichDesignByteArray");
        return target.getRichDesignByteArray();
    }

    @PendingRemoteMethod
    public void changePageSize(int groupID, int pageSize) throws RemoteException {
        logRemoteMethodCall("changePageSize");
        target.changePageSize(groupID, pageSize);
    }

    @PendingRemoteMethod
    public void gainedFocus() throws RemoteException {
        logRemoteMethodCall("gainedFocus");
        target.gainedFocus();
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte[] value) throws RemoteException {
        logRemoteMethodCall("changeGroupObject");
        target.changeGroupObject(groupID, value);
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte changeType) throws RemoteException {
        logRemoteMethodCall("changeGroupObject");
        target.changeGroupObject(groupID, changeType);
    }

    @PendingRemoteMethod
    public void changePropertyDraw(int propertyID, byte[] object, boolean all) throws RemoteException {
        logRemoteMethodCall("changePropertyDraw");
        target.changePropertyDraw(propertyID, object, all);
    }

    @PendingRemoteMethod
    public void changeObject(int objectID, Object value) throws RemoteException {
        logRemoteMethodCall("changeObject");
        target.changeObject(objectID, value);
    }

    @PendingRemoteMethod
    public void addObject(int objectID, int classID) throws RemoteException {
        logRemoteMethodCall("addObject");
        target.addObject(objectID, classID);
    }

    @PendingRemoteMethod
    public void changeClass(int objectID, int classID) throws RemoteException {
        logRemoteMethodCall("changeClass");
        target.changeClass(objectID, classID);
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        logRemoteMethodCall("canChangeClass");
        return target.canChangeClass(objectID);
    }

    @PendingRemoteMethod
    public void changeGridClass(int objectID, int idClass) throws RemoteException {
        logRemoteMethodCall("changeGridClass");
        target.changeGridClass(objectID, idClass);
    }

    @PendingRemoteMethod
    public void switchClassView(int groupID) throws RemoteException {
        logRemoteMethodCall("switchClassView");
        target.switchClassView(groupID);
    }

    @PendingRemoteMethod
    public void changeClassView(int groupID, byte classView) throws RemoteException {
        logRemoteMethodCall("changeClassView");
        target.changeClassView(groupID, classView);
    }

    @PendingRemoteMethod
    public void changePropertyOrder(int propertyID, byte modiType) throws RemoteException {
        logRemoteMethodCall("changePropertyOrder");
        target.changePropertyOrder(propertyID, modiType);
    }

    @PendingRemoteMethod
    public void changeObjectOrder(int propertyID, byte modiType) throws RemoteException {
        logRemoteMethodCall("changeObjectOrder");
        target.changeObjectOrder(propertyID, modiType);
    }

    @PendingRemoteMethod
    public void changeObjectClassOrder(int propertyID, byte modiType) throws RemoteException {
        logRemoteMethodCall("changeObjectClassOrder");
        target.changeObjectClassOrder(propertyID, modiType);
    }

    @PendingRemoteMethod
    public void clearUserFilters() throws RemoteException {
        logRemoteMethodCall("clearUserFilters");
        target.clearUserFilters();
    }

    @PendingRemoteMethod
    public void addFilter(byte[] state) throws RemoteException {
        logRemoteMethodCall("addFilter");
        target.addFilter(state);
    }

    @PendingRemoteMethod
    public void setRegularFilter(int groupID, int filterID) throws RemoteException {
        logRemoteMethodCall("setRegularFilter");
        target.setRegularFilter(groupID, filterID);
    }

    @ImmutableMethod
    public int getID() throws RemoteException {
        logRemoteMethodCall("getID");
        return target.getID();
    }

    @PendingRemoteMethod
    public void refreshData() throws RemoteException {
        logRemoteMethodCall("refreshData");
        target.refreshData();
    }

    @ImmutableMethod
    public boolean hasClientApply() throws RemoteException {
        logRemoteMethodCall("hasClientApply");
        return target.hasClientApply();
    }

    public ClientApply getClientApply() throws RemoteException {
        logRemoteMethodCall("getClientApply");
        return target.getClientApply();
    }

    @PendingRemoteMethod
    public void applyClientChanges(Object clientResult) throws RemoteException {
        logRemoteMethodCall("applyClientChanges");
        target.applyClientChanges(clientResult);
    }

    @PendingRemoteMethod
    public void applyChanges() throws RemoteException {
        logRemoteMethodCall("applyChanges");
        target.applyChanges();
    }

    @PendingRemoteMethod
    public void cancelChanges() throws RemoteException {
        logRemoteMethodCall("cancelChanges");
        target.cancelChanges();
    }

    public byte[] getPropertyChangeType(int propertyID) throws RemoteException {
        logRemoteMethodCall("getPropertyChangeType");
        return target.getPropertyChangeType(propertyID);
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
