package lsfusion.interop.logics;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.exception.AppServerNotAvailableException;
import lsfusion.interop.exception.AuthenticationException;
import lsfusion.interop.session.SessionInfo;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractLogicsProviderImpl {

    // not like in other providers, getter shouldn't be called directly to ensure invalidating reference if we get RemoteException
    private LogicsSessionObject createLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException {
        LogicsSessionObject logicsSessionObject;
        logicsSessionObject = new LogicsSessionObject(lookup(connection), connection);
        return logicsSessionObject;
    }

    protected RemoteLogicsInterface lookup(LogicsConnection connection) throws AppServerNotAvailableException {
        RemoteLogicsInterface logics;
        try {
            RemoteLogicsLoaderInterface loader = RMIUtils.rmiLookup(connection.host, connection.port, connection.exportName, "RemoteLogicsLoader");
            logics = loader.getLogics();
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        } catch (NotBoundException | RemoteException e) {
            throw new AppServerNotAvailableException("Application server [" + connection.host + ":" + connection.port + "(" + connection.exportName + ")] is not available. Reason : " + ExceptionUtils.copyMessage(e));
        }
        return logics;
    }

    private final Map<LogicsConnection, LogicsSessionObject> currentLogics = new ConcurrentHashMap<>();

    private LogicsSessionObject createOrGetLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException {
        LogicsSessionObject logicsSessionObject = currentLogics.get(connection);
        if(logicsSessionObject == null) { // no sync, it's no big deal if we'll lost some cache
            logicsSessionObject = createLogicsSessionObject(connection);
            currentLogics.put(connection, logicsSessionObject);
        }
        return logicsSessionObject;
    }

    private void invalidateLogicsSessionObject(LogicsSessionObject sessionObject) { // should be called if logics remote method call fails with remoteException
        currentLogics.remove(sessionObject.connection);
    }

    public <R> R runRequest(LogicsConnection connection, LogicsRunnable<R> runnable) throws AppServerNotAvailableException, RemoteException {
        LogicsSessionObject logicsSessionObject = createOrGetLogicsSessionObject(connection);
        try {
            return runnable.run(logicsSessionObject);
        } catch (AuthenticationException e) {
            // if there is an AuthenticationException and server has anonymousUI, that means that the mode has changed, so we we'll drop serverSettings cache
            if(logicsSessionObject.serverSettings != null && logicsSessionObject.serverSettings.anonymousUI)
                logicsSessionObject.serverSettings = null;
            throw e;
        } catch (RemoteException e) { // it's important that this exception should not be suppressed (for example in ExternalRequestHandler)
            invalidateLogicsSessionObject(logicsSessionObject);
            throw e;
        }
    }

    public ServerSettings getServerSettings(LogicsConnection connection, final SessionInfo sessionInfo, final String contextPath) {
        try {
            return runRequest(connection, new LogicsRunnable<ServerSettings>() {
                public ServerSettings run(LogicsSessionObject sessionObject) throws RemoteException {
                    return sessionObject.getServerSettings(sessionInfo, contextPath);
                }
            });
        } catch (Throwable t) {
            return null;
        }
    }
}
