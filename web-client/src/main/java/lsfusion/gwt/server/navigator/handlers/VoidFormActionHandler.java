package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.client.base.exception.AppServerNotAvailableDispatchException;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidFormAction;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class VoidFormActionHandler extends FormActionHandler<VoidFormAction, VoidResult> {
    public VoidFormActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(VoidFormAction action, ExecutionContext context) throws RemoteException, AppServerNotAvailableDispatchException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        if (form != null)
            form.remoteForm.voidFormAction(action.requestIndex, action.lastReceivedRequestIndex);
        return new VoidResult();
    }
}