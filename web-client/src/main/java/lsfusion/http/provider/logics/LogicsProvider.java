package lsfusion.http.provider.logics;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.interop.connection.AuthenticationToken;
import lsfusion.interop.logics.LogicsRunnable;
import lsfusion.interop.logics.ServerSettings;
import lsfusion.interop.navigator.ClientSettings;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface LogicsProvider {

    ServerSettings getServerSettings(HttpServletRequest request, boolean noCache);

    ClientSettings getClientSettings(HttpServletRequest request, AuthenticationToken token);

    <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException;
    <R> R runRequest(HttpServletRequest request, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableDispatchException;
}
