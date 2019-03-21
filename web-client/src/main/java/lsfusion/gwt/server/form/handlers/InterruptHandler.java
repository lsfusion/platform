package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.form.Interrupt;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class InterruptHandler extends FormActionHandler<Interrupt, VoidResult> {

    public InterruptHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(Interrupt action, ExecutionContext context) throws RemoteException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        if (form != null)
            form.remoteForm.interrupt(action.cancelable);
        return new VoidResult();
    }
}