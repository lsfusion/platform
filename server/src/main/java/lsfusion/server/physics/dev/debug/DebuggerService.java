package lsfusion.server.physics.dev.debug;

import java.rmi.Remote;
import java.rmi.RemoteException;

// should be in the same package and with the same interface as DebuggerService in plugin-idea
public interface DebuggerService extends Remote {
    void registerBreakpoint(String module, Integer line) throws RemoteException;

    void unregisterBreakpoint(String module, Integer line) throws RemoteException;

    void registerStepping() throws RemoteException;

    void unregisterStepping() throws RemoteException;

    void showFormDesign(String form, String formName) throws RemoteException;
}
