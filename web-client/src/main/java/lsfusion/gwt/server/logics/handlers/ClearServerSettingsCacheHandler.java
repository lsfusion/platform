package lsfusion.gwt.server.logics.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.logics.ClearServerSettingsCache;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.logics.LogicsActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ClearServerSettingsCacheHandler extends LogicsActionHandler<ClearServerSettingsCache, VoidResult> {
    public ClearServerSettingsCacheHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(ClearServerSettingsCache action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        return runRequest(action, (sessionObject, retry) -> {
            sessionObject.serverSettings = null;
            return new VoidResult();
        });
    }
}
