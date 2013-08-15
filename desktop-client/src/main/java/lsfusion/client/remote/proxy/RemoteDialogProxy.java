package lsfusion.client.remote.proxy;

import lsfusion.interop.form.RemoteDialogInterface;
import lsfusion.interop.remote.SelectedObject;

import java.rmi.RemoteException;

public class RemoteDialogProxy extends RemoteFormProxy<RemoteDialogInterface> implements RemoteDialogInterface {

    public RemoteDialogProxy(RemoteDialogInterface target) {
        super(target);
    }

    @Override
    public SelectedObject getSelectedObject() throws RemoteException {
        logRemoteMethodStartCall("getSelectedObject");
        SelectedObject result = target.getSelectedObject();
        logRemoteMethodEndCall("getSelectedObject", result);
        return result;
    }

    @ImmutableMethod
    public Integer getInitFilterPropertyDraw() throws RemoteException {
        logRemoteMethodStartCall("getInitFilterPropertyDraw");
        Integer result = target.getInitFilterPropertyDraw();
        logRemoteMethodEndCall("getInitFilterPropertyDraw", result);
        return result;
    }

    @ImmutableMethod
    public Boolean isUndecorated() throws RemoteException {
        logRemoteMethodStartCall("isUndecorated");
        Boolean result = target.isUndecorated();
        logRemoteMethodEndCall("isUndecorated", result);
        return result;
    }
}
