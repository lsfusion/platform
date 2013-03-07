package paas.terminal;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ApplicationTerminal extends Remote {
    public void stop() throws RemoteException;
}
