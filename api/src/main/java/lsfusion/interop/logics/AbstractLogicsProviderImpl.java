package lsfusion.interop.logics;

import lsfusion.base.ExceptionUtils;
import lsfusion.base.remote.RMIUtils;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.base.exception.AuthenticationException;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;
import lsfusion.interop.logics.remote.RemoteLogicsLoaderInterface;
import lsfusion.interop.session.SessionInfo;

import java.net.MalformedURLException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.*;

public abstract class AbstractLogicsProviderImpl {

    // not like in other providers, getter shouldn't be called directly to ensure invalidating reference if we get RemoteException
    private LogicsSessionObject createLogicsSessionObject(LogicsConnection connection) throws AppServerNotAvailableException {
        LogicsSessionObject logicsSessionObject;
        logicsSessionObject = new LogicsSessionObject(lookup(connection), connection);
        return logicsSessionObject;
    }

    protected RemoteLogicsInterface lookup(final LogicsConnection connection) throws AppServerNotAvailableException {
        RemoteLogicsInterface logics;
        try {

            final Future<RemoteLogicsLoaderInterface> future = Executors.newSingleThreadExecutor().submit((Callable) () -> lookupLoader(connection));

            RemoteLogicsLoaderInterface loader;
            try {
                loader = future.get(2000, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw new AppServerNotAvailableException("Application server [" + connection.host + ":" + connection.port + "(" + connection.exportName + ")] is not available. Reason: Timeout");
            }
            logics = loader.getLogics();
        }  catch (RemoteException | InterruptedException | ExecutionException e) {
            throw new AppServerNotAvailableException("Application server [" + connection.host + ":" + connection.port + "(" + connection.exportName + ")] is not available. Reason: " + ExceptionUtils.copyMessage(e));
        }
        return logics;
    }

    protected RemoteLogicsLoaderInterface lookupLoader(LogicsConnection connection) throws RemoteException, NotBoundException, MalformedURLException {
        return RMIUtils.rmiLookup(connection.host, connection.port, connection.exportName, "RemoteLogicsLoader");
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
        return runRequest(connection, runnable, false);
    }

    private <R> R runRequest(LogicsConnection connection, LogicsRunnable<R> runnable, boolean retry) throws AppServerNotAvailableException, RemoteException {
        LogicsSessionObject logicsSessionObject = createOrGetLogicsSessionObject(connection);
        try {
            return runnable.run(logicsSessionObject, retry);
        } catch (AuthenticationException e) {
            // if there is an AuthenticationException and server has anonymousUI, that means that the mode has changed, so we we'll drop serverSettings cache
            if(logicsSessionObject.serverSettings != null && logicsSessionObject.serverSettings.anonymousUI)
                logicsSessionObject.serverSettings = null;
            throw e;
        } catch (RemoteException e) { // it's important that this exception should not be suppressed (for example in ExternalRequestHandler)
            invalidateLogicsSessionObject(logicsSessionObject);
            if (e instanceof NoSuchObjectException && !retry)
                return runRequest(connection, runnable, true);
            else
                throw e;
        }
    }

    public ServerSettings getServerSettings(LogicsConnection connection, final SessionInfo sessionInfo, final String contextPath, final boolean noCache) {
        try {
            return runRequest(connection, (sessionObject, retry) -> sessionObject.getServerSettings(sessionInfo, contextPath, noCache));
        } catch (Throwable t) {
//            throw Throwables.propagate(t);
            return null;
        }
    }
}
