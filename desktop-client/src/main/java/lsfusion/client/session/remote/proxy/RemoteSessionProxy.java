package lsfusion.client.session.remote.proxy;

import lsfusion.client.controller.remote.proxy.PendingRemoteObjectProxy;
import lsfusion.client.controller.remote.proxy.RemoteRequestObjectProxy;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.remote.RemoteSessionInterface;

import java.rmi.RemoteException;

public class RemoteSessionProxy<T extends RemoteSessionInterface> extends RemoteRequestObjectProxy<T> implements RemoteSessionInterface {

    public RemoteSessionProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("exec");
        ExternalResponse result = target.exec(action, request);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ExternalResponse eval(boolean action, Object paramScript, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("eval");
        ExternalResponse result = target.eval(action, paramScript, request);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }
}
