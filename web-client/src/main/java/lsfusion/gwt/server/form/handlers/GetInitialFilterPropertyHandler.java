package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.result.NumberResult;
import lsfusion.gwt.client.controller.remote.action.form.GetInitialFilterProperty;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetInitialFilterPropertyHandler extends FormActionHandler<GetInitialFilterProperty, NumberResult> {
    public GetInitialFilterPropertyHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(GetInitialFilterProperty action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return form.remoteForm == null ? null : new NumberResult(form.remoteForm.getInitFilterPropertyDraw());
    }
}
