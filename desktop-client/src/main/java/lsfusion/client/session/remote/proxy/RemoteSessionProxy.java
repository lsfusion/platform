package lsfusion.client.session.remote.proxy;

import lsfusion.client.connection.RemoteConnectionProxy;
import lsfusion.interop.session.ExternalRequest;
import lsfusion.interop.session.ExternalResponse;
import lsfusion.interop.session.remote.RemoteSessionInterface;

import java.rmi.RemoteException;

public class RemoteSessionProxy<T extends RemoteSessionInterface> extends RemoteConnectionProxy<T> implements RemoteSessionInterface {

    public RemoteSessionProxy(T target, String realHostName) {
        super(target, realHostName);
    }

    @Override
    public ExternalResponse exec(String action, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("exec");
        ExternalResponse result = super.exec(action, request);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ExternalResponse eval(boolean action, ExternalRequest.Param paramScript, ExternalRequest request) throws RemoteException {
        logRemoteMethodStartCall("eval");
        ExternalResponse result = super.eval(action, paramScript, request);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }
}
