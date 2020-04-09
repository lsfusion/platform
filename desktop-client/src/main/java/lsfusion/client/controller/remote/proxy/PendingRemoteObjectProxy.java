package lsfusion.client.controller.remote.proxy;

import lsfusion.interop.base.remote.PendingRemoteInterface;

import java.rmi.RemoteException;
import java.util.List;

public abstract class PendingRemoteObjectProxy<T extends PendingRemoteInterface> extends RemoteObjectProxy<T> implements PendingRemoteInterface {

    public PendingRemoteObjectProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public String getRemoteActionMessage() throws RemoteException {
        return target.getRemoteActionMessage();
    }

    @Override
    public List<Object> getRemoteActionMessageList() throws RemoteException {
        return target.getRemoteActionMessageList();
    }

    @Override
    public void interrupt(boolean cancelable) throws RemoteException {
        target.interrupt(cancelable);
    }

    @Override
    public void close() throws RemoteException {
        target.close();
    }
}
