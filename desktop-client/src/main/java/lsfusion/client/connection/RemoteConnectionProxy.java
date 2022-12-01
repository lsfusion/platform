package lsfusion.client.connection;

import lsfusion.client.controller.remote.proxy.RemoteRequestObjectProxy;
import lsfusion.interop.connection.RemoteConnectionInterface;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;

import java.rmi.RemoteException;

public class RemoteConnectionProxy<T extends RemoteConnectionInterface> extends RemoteRequestObjectProxy<T> implements RemoteConnectionInterface {

    public RemoteConnectionProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
        return target.exec(action, request);
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException {
        return target.eval(action, paramScript, request);
    }
}
