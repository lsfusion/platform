package lsfusion.gwt.form.server.logics.spring;

import lsfusion.gwt.base.server.exceptions.AppServerNotAvailableException;
import lsfusion.gwt.base.server.spring.InvalidateListener;
import lsfusion.gwt.form.server.logics.LogicsConnection;
import lsfusion.gwt.form.server.navigator.spring.LogicsAndNavigatorProvider;
import lsfusion.interop.RemoteLogicsInterface;

import java.io.IOException;
import java.rmi.RemoteException;

public interface LogicsHandlerProvider {

    // read default params, if some are not set
    LogicsConnection getLogicsConnection(String host, Integer port, String exportName);

    RemoteLogicsInterface getLogics(LogicsConnection connection) throws AppServerNotAvailableException;
    void invalidate(LogicsConnection connection);
}
