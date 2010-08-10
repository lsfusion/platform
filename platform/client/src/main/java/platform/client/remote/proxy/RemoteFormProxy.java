package platform.client.remote.proxy;

import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionResult;
import platform.interop.form.RemoteChanges;
import platform.interop.form.RemoteDialogInterface;
import platform.interop.form.RemoteFormInterface;

import java.rmi.RemoteException;
import java.util.List;

public class RemoteFormProxy<T extends RemoteFormInterface>
        extends RemoteObjectProxy<T>
        implements RemoteFormInterface {

    public RemoteFormProxy(T target) {
        super(target);
    }

    public byte[] getReportDesignByteArray(boolean toExcel) throws RemoteException {
        return target.getReportDesignByteArray(toExcel);
    }

    public boolean hasCustomReportDesign() throws RemoteException {
        return target.hasCustomReportDesign();
    }

    public byte[] getReportDataByteArray() throws RemoteException {
        return target.getReportDataByteArray();
    }

    public RemoteChanges getRemoteChanges() throws RemoteException {
        return target.getRemoteChanges();
    }

    public byte[] getRichDesignByteArray() throws RemoteException {
        return target.getRichDesignByteArray();
    }

    @PendingRemoteMethod
    public void changePageSize(int groupID, int pageSize) throws RemoteException {
        target.changePageSize(groupID, pageSize);
    }

    @PendingRemoteMethod
    public void gainedFocus() throws RemoteException {
        target.gainedFocus();
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte[] value) throws RemoteException {
        target.changeGroupObject(groupID, value);
    }

    @PendingRemoteMethod
    public void changeGroupObject(int groupID, byte changeType) throws RemoteException {
        target.changeGroupObject(groupID, changeType);
    }

    @PendingRemoteMethod
    public void changePropertyView(int propertyID, byte[] object, boolean all) throws RemoteException {
        target.changePropertyView(propertyID, object, all);
    }

    @PendingRemoteMethod
    public void changeObject(int objectID, Object value) throws RemoteException {
        target.changeObject(objectID, value);
    }

    @PendingRemoteMethod
    public void addObject(int objectID, int classID) throws RemoteException {
        target.addObject(objectID, classID);
    }

    @PendingRemoteMethod
    public void changeClass(int objectID, int classID) throws RemoteException {
        target.changeClass(objectID, classID);
    }

    public boolean canChangeClass(int objectID) throws RemoteException {
        return target.canChangeClass(objectID);
    }

    @PendingRemoteMethod
    public void changeGridClass(int objectID, int idClass) throws RemoteException {
        target.changeGridClass(objectID, idClass);
    }

    @PendingRemoteMethod
    public void switchClassView(int groupID) throws RemoteException {
        target.switchClassView(groupID);
    }

    @PendingRemoteMethod
    public void changeClassView(int groupID, byte classView) throws RemoteException {
        target.changeClassView(groupID, classView);
    }

    @PendingRemoteMethod
    public void changePropertyOrder(int propertyID, byte modiType) throws RemoteException {
        target.changePropertyOrder(propertyID, modiType);
    }

    @PendingRemoteMethod
    public void changeObjectOrder(int propertyID, byte modiType) throws RemoteException {
        target.changeObjectOrder(propertyID, modiType);
    }

    @PendingRemoteMethod
    public void clearUserFilters() throws RemoteException {
        target.clearUserFilters();
    }

    @PendingRemoteMethod
    public void addFilter(byte[] state) throws RemoteException {
        target.addFilter(state);
    }

    @PendingRemoteMethod
    public void setRegularFilter(int groupID, int filterID) throws RemoteException {
        target.setRegularFilter(groupID, filterID);
    }

    public int getID() throws RemoteException {
        return target.getID();
    }

    @PendingRemoteMethod
    public void refreshData() throws RemoteException {
        target.refreshData();
    }

    public List<? extends ClientAction> getApplyActions() throws RemoteException {
        return target.getApplyActions();
    }

    public String checkApplyActions(int actionID, ClientActionResult result) throws RemoteException {
        return target.checkApplyActions(actionID, result);
    }

    public String checkChanges() throws RemoteException {
        return target.checkChanges();
    }

    public String applyChanges() throws RemoteException {
        return target.applyChanges();
    }

    @PendingRemoteMethod
    public void cancelChanges() throws RemoteException {
        target.cancelChanges();
    }

    public byte[] getPropertyChangeType(int propertyID) throws RemoteException {
        return target.getPropertyChangeType(propertyID);
    }

    public RemoteDialogInterface createClassPropertyDialog(int viewID, int value) throws RemoteException {
        return new RemoteDialogProxy(target.createClassPropertyDialog(viewID, value));
    }

    public RemoteDialogInterface createEditorPropertyDialog(int viewID) throws RemoteException {
        return new RemoteDialogProxy(target.createEditorPropertyDialog(viewID));
    }

    public RemoteDialogInterface createObjectDialog(int objectID) throws RemoteException {
        return new RemoteDialogProxy(target.createObjectDialog(objectID));
    }

    public RemoteDialogInterface createObjectDialog(int objectID, int value) throws RemoteException {
        return new RemoteDialogProxy(target.createObjectDialog(objectID, value));
    }
}
