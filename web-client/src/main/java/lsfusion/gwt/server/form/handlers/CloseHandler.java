package lsfusion.gwt.server.form.handlers;

import lsfusion.base.BaseUtils;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.form.Close;
import lsfusion.gwt.client.controller.remote.action.form.GetRemoteActionMessage;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import lsfusion.http.provider.form.FormProviderImpl;
import lsfusion.http.provider.form.FormSessionObject;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.rmi.RemoteException;

public class CloseHandler extends FormActionHandler<Close, VoidResult> {
    public CloseHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(Close action, ExecutionContext context) throws RemoteException {
        try {
            getFormSessionObject(action.formSessionID).remoteForm.close();
        } catch (SessionInvalidatedException e) {
        } finally {
            getFormProvider().scheduleRemoveFormSessionObject(action.formSessionID, action.closeDelay);
        }
        return new VoidResult();
    }
}
