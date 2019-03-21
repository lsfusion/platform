package lsfusion.client.logics;

import lsfusion.client.controller.remote.proxy.RemoteLogicsProxy;
import lsfusion.interop.base.exception.AppServerNotAvailableException;
import lsfusion.interop.logics.AbstractLogicsProviderImpl;
import lsfusion.interop.logics.LogicsConnection;
import lsfusion.interop.logics.remote.RemoteLogicsInterface;

public class LogicsProvider extends AbstractLogicsProviderImpl {
    
    public static LogicsProvider instance = new LogicsProvider();

    @Override
    protected RemoteLogicsInterface lookup(LogicsConnection connection) throws AppServerNotAvailableException {
        return new RemoteLogicsProxy<>(super.lookup(connection));
    }
}
