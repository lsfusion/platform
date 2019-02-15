package lsfusion.interop.session;

import lsfusion.base.ExecResult;
import lsfusion.interop.remote.AuthenticationToken;
import lsfusion.interop.remote.PendingRemoteInterface;

import java.rmi.RemoteException;

public interface RemoteSessionInterface extends PendingRemoteInterface {

    // main interface

    // external requests (interface is similar to RemoteLogicsInterface but without token)
    ExecResult exec(String action, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;
    ExecResult eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;

}
