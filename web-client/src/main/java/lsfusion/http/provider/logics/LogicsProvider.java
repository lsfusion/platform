package lsfusion.http.provider.logics;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.ServerSettings;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface LogicsProvider {

    ServerSettings getServerSettings(HttpServletRequest request, boolean noCache);

    <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException;
    <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException;
}
