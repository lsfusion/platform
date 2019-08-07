package lsfusion.client.logics;

import lsfusion.client.controller.remote.proxy.RemoteLogicsLoaderProxy;
import lsfusion.interop.logics.AbstractLogicsProviderImpl;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class LogicsProvider extends AbstractLogicsProviderImpl {
    
    public static LogicsProvider instance = new LogicsProvider();

    @Override
    protected RemoteLogicsLoaderInterface lookupLoader(LogicsConnection connection) throws RemoteException, NotBoundException, MalformedURLException {
        return new RemoteLogicsLoaderProxy(super.lookupLoader(connection), connection.host);
    }
}
