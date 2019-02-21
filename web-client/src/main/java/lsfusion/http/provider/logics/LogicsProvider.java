package lsfusion.http.provider.logics;

import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;

import javax.servlet.http.HttpServletRequest;
import java.rmi.RemoteException;

public interface LogicsProvider {

    ServerSettings getServerSettings(HttpServletRequest request);
    
    String getJnlpUrls(HttpServletRequest request);

    <R> R runRequest(String host, Integer port, String exportName, LogicsRunnable<R> runnable) throws RemoteException, AppServerNotAvailableException;

}
