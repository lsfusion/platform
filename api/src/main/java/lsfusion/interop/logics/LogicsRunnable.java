package lsfusion.interop.logics;

import java.rmi.RemoteException;

public interface LogicsRunnable<R> {

    // it's important that all remoteexception should be rethrown
    R run(LogicsSessionObject sessionObject, boolean retry) throws RemoteException;
}
