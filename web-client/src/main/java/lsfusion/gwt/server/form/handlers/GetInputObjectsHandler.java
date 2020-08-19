package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.GetInputObjects;
import lsfusion.gwt.client.controller.remote.action.form.InputObjectsResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class GetInputObjectsHandler extends FormActionHandler<GetInputObjects, InputObjectsResult> {
    public GetInputObjectsHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public InputObjectsResult executeEx(GetInputObjects action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return form.remoteForm == null ? null : new InputObjectsResult(form.remoteForm.getInputObjects());
    }
}