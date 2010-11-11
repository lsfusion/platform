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
}
