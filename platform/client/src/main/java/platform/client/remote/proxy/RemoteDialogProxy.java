package platform.client.remote.proxy;

import platform.interop.form.RemoteDialogInterface;

import java.rmi.RemoteException;

public class RemoteDialogProxy<T extends RemoteDialogInterface>
        extends RemoteFormProxy<T>
        implements RemoteDialogInterface {

    public RemoteDialogProxy(T target) {
        super(target);
    }

    public Object getDialogValue() throws RemoteException {
        logRemoteMethodStartCall("getDialogValue");
        Object result = target.getDialogValue();
        logRemoteMethodEndCall("getDialogValue", result);
        return result;
    }

    @ImmutableMethod
    public Integer getInitFilterPropertyDraw() throws RemoteException {
        logRemoteMethodStartCall("getInitFilterPropertyDraw");
        Integer result = target.getInitFilterPropertyDraw();
        logRemoteMethodEndCall("getInitFilterPropertyDraw", result);
        return result;
    }
}
