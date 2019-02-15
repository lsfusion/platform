package lsfusion.http;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.http.provider.logics.LogicsHandlerProvider;
import lsfusion.interop.RemoteLogicsInterface;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.rmi.RemoteException;

// native interfaces because either we need to use spring, or we can't use gwt
public class LogicsRequestHandler {

    @Autowired
    protected LogicsHandlerProvider logicsHandlerProvider;

    protected <R> R runRequest(HttpServletRequest request, Runnable<R> runnable) throws IOException {
        try {
            return runRequest(logicsHandlerProvider, request, runnable);
        } catch (AppServerNotAvailableException e) {
            throw Throwables.propagate(e);
        }
    }

    public interface Runnable<R> {
        R run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws IOException;
    }

    public static <R> R runRequest(LogicsHandlerProvider logicsHandlerProvider, HttpServletRequest request, Runnable<R> runnable) throws IOException, AppServerNotAvailableException {
        return runRequest(logicsHandlerProvider, 
                        request != null ? request.getParameter("host") : null, 
                        request != null ? BaseUtils.parseInt(request.getParameter("port")) : null, 
                        request != null ? request.getParameter("exportName") : null, 
                        runnable);
    }
    public static <R> R runRequest(LogicsHandlerProvider logicsHandlerProvider, String host, Integer port, String exportName, Runnable<R> runnable) throws IOException, AppServerNotAvailableException {
        LogicsConnection logicsConnection = logicsHandlerProvider.getLogicsConnection(host, port, exportName);
        RemoteLogicsInterface remoteLogics = logicsHandlerProvider.getLogics(logicsConnection);
        try {
            return runnable.run(remoteLogics, logicsConnection);
        } catch (RemoteException e) {
            logicsHandlerProvider.invalidate(logicsConnection);
            throw e;
        }
    }
}
