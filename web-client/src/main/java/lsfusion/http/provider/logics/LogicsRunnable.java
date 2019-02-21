package lsfusion.http.provider.logics;

import lsfusion.http.provider.logics.LogicsSessionObject;

import java.io.IOException;
import java.rmi.RemoteException;

public interface LogicsRunnable<R> {

    // it's important that all remoteexception should be rethrown
    R run(LogicsSessionObject sessionObject) throws RemoteException;
}
