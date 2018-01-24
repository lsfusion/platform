package lsfusion.client;

import java.rmi.RemoteException;

public interface EditReportInvoker {
    void invokeAddReport() throws RemoteException;
    void invokeEditReport() throws RemoteException;
    void invokeDeleteReport() throws RemoteException;
    boolean hasCustomReports() throws RemoteException;
}
