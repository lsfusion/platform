package lsfusion.client;

import java.rmi.RemoteException;

public interface EditReportInvoker {
    void invokeEditReport(boolean useAuto) throws RemoteException;
    void invokeDeleteReport() throws RemoteException;
    boolean hasCustomReports() throws RemoteException;
}
