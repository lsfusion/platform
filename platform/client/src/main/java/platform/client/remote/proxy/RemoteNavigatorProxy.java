package platform.client.remote.proxy;

import platform.interop.form.RemoteFormInterface;
import platform.interop.navigator.RemoteNavigatorInterface;

import java.rmi.RemoteException;

public class RemoteNavigatorProxy<T extends RemoteNavigatorInterface>
        extends RemoteObjectProxy<T>
        implements RemoteNavigatorInterface {

    public RemoteNavigatorProxy(T target) {
        super(target);
    }

    public String getForms(String formSet) throws RemoteException {
        return target.getForms(formSet);
    }

    public RemoteFormInterface createForm(int formID, boolean currentSession) throws RemoteException {
        return new RemoteFormProxy(target.createForm(formID, currentSession));
    }

    public byte[] getCurrentUserInfoByteArray() throws RemoteException {
        return target.getCurrentUserInfoByteArray();
    }

    public byte[] getElementsByteArray(int groupID) throws RemoteException {
        return target.getElementsByteArray(groupID);
    }
}
