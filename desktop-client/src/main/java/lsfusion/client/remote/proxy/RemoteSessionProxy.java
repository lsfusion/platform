package lsfusion.client.remote.proxy;

import lsfusion.base.ExecResult;
import lsfusion.base.SessionInfo;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import lsfusion.interop.remote.AuthenticationToken;
import lsfusion.interop.session.RemoteSessionInterface;

import java.rmi.RemoteException;

public class RemoteSessionProxy<T extends RemoteSessionInterface> extends RemoteObjectProxy<T> implements RemoteSessionInterface {

    public RemoteSessionProxy(T target) {
        super(target);
    }

    @Override
    public ExecResult exec(String action, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException {
        logRemoteMethodStartCall("exec");
        ExecResult result = target.exec(action, returnCanonicalNames, params, charset, headerNames, headerValues);
        logRemoteMethodEndVoidCall("exec");
        return result;
    }

    @Override
    public ExecResult eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException {
        logRemoteMethodStartCall("eval");
        ExecResult result = target.eval(action, paramScript, returnCanonicalNames, params, charset, headerNames, headerValues);
        logRemoteMethodEndVoidCall("eval");
        return result;
    }
}
