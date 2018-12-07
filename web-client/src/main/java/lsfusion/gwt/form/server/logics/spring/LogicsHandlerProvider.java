package lsfusion.gwt.form.server.logics.spring;

import lsfusion.gwt.base.shared.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.form.server.logics.LogicsConnection;
import lsfusion.interop.RemoteLogicsInterface;

public interface LogicsHandlerProvider {

    // read default params, if some are not set
    LogicsConnection getLogicsConnection(String host, Integer port, String exportName);

    RemoteLogicsInterface getLogics(LogicsConnection connection) throws AppServerNotAvailableException;
    void invalidate(LogicsConnection connection);
}
