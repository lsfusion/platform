package lsfusion.client.remote.proxy;

import com.google.common.base.Throwables;
import lsfusion.interop.form.RemoteDialogInterface;
import lsfusion.interop.remote.SelectedObject;

import java.rmi.RemoteException;
import java.util.concurrent.Callable;

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

    @ImmutableMethod
    public Boolean isUndecorated() throws RemoteException {
        try {
            return callImmutableMethod("isUndecorated", new Callable<Boolean>() {
                @Override
                public Boolean call() throws Exception {
                    logRemoteMethodStartCall("isUndecorated");
                    Boolean result = target.isUndecorated();
                    logRemoteMethodEndCall("isUndecorated", result);
                    return result;
                }
            });
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }
}
