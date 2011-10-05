package platform.server.logics;

import platform.interop.remote.ApplicationTerminal;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ApplicationTerminalImpl extends UnicastRemoteObject implements ApplicationTerminal {
    public ApplicationTerminalImpl(int exportPort) throws RemoteException {
        super(exportPort);
    }

    @Override
    public void stop() {
        try {
            BusinessLogicsBootstrap.stop();
        } catch (RemoteException e) {
            e.printStackTrace();            
        } catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
