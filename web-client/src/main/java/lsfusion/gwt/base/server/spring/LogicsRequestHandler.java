package lsfusion.gwt.base.server.spring;

import com.google.common.base.Throwables;
import lsfusion.gwt.base.server.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.form.server.logics.LogicsConnection;
import lsfusion.gwt.form.server.logics.spring.LogicsHandlerProvider;
import lsfusion.interop.RemoteLogicsInterface;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.rmi.RemoteException;

// native interfaces because either we need to use spring, or we can't use gwt
public class LogicsRequestHandler {

    @Autowired
    protected LogicsHandlerProvider logicsHandlerProvider;

    protected <R> R runRequest(String host, Integer port, String exportName, Runnable<R> runnable) throws IOException {
        try {
            return runRequest(logicsHandlerProvider, host, port, exportName, runnable);
        } catch (AppServerNotAvailableException e) {
            throw Throwables.propagate(e);
        }
    }

    public interface Runnable<R> {
        R run(RemoteLogicsInterface remoteLogics, LogicsConnection logicsConnection) throws IOException;
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
