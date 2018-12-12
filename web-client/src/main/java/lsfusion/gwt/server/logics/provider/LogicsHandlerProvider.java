package lsfusion.gwt.server.logics.provider;

import lsfusion.gwt.shared.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;

public interface LogicsHandlerProvider {

    // read default params, if some are not set
    LogicsConnection getLogicsConnection(String host, Integer port, String exportName);

    RemoteLogicsInterface getLogics(LogicsConnection connection) throws AppServerNotAvailableException;
    void invalidate(LogicsConnection connection);
}
