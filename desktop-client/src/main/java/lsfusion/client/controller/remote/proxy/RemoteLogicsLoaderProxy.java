package lsfusion.client.controller.remote.proxy;

import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;

import java.rmi.RemoteException;

public class RemoteLogicsLoaderProxy extends RemoteObjectProxy<RemoteLogicsLoaderInterface> implements RemoteLogicsLoaderInterface {

    public RemoteLogicsLoaderProxy(RemoteLogicsLoaderInterface target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public RemoteLogicsInterface getLogics() throws RemoteException {
        return new RemoteLogicsProxy<>(target.getLogics(), realHostName);
    }
}
