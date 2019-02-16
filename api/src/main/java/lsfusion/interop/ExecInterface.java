package lsfusion.interop;

import lsfusion.base.ExecResult;

import java.rmi.RemoteException;

public interface ExecInterface {

    // external requests
    ExecResult exec(String action, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;
    ExecResult eval(boolean action, Object paramScript, String[] returnCanonicalNames, Object[] params, String charset, String[] headerNames, String[] headerValues) throws RemoteException;    
}
