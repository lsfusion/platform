package lsfusion.server.logics.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DebuggerService extends Remote {
    void registerBreakpoint(String module, Integer line) throws RemoteException;

    void unregisterBreakpoint(String module, Integer line) throws RemoteException;

    void registerStepping() throws RemoteException;

    void unregisterStepping() throws RemoteException;
}
