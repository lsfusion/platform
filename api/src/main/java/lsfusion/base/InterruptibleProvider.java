package lsfusion.base;

import java.rmi.RemoteException;

public interface InterruptibleProvider<T> extends Provider<T> {
    void interrupt(boolean cancelable) throws RemoteException;
}
