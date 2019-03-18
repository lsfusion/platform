package lsfusion.client.session.remote.proxy;

import lsfusion.client.remote.proxy.RemoteObjectProxy;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.RemoteSessionInterface;

import java.rmi.RemoteException;

public class RemoteSessionProxy<T extends RemoteSessionInterface> extends RemoteObjectProxy<T> implements RemoteSessionInterface {

    public RemoteSessionProxy(T target) {
        super(target);
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

    public void close() throws RemoteException {
        logRemoteMethodStartCall("close");
        target.close();
        logRemoteMethodEndCall("close", "");
    }
}
