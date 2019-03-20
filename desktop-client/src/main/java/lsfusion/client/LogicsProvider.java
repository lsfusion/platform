package lsfusion.client;

import lsfusion.client.controller.remote.proxy.RemoteLogicsProxy;
import lsfusion.interop.exception.AppServerNotAvailableException;
import lsfusion.interop.logics.*;

public class LogicsProvider extends AbstractLogicsProviderImpl {
    
    public static LogicsProvider instance = new LogicsProvider();

    @Override
    protected RemoteLogicsInterface lookup(LogicsConnection connection) throws AppServerNotAvailableException {
        return new RemoteLogicsProxy<>(super.lookup(connection));
    }
}
