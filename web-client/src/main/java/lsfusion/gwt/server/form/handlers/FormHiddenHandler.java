package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.controller.remote.action.form.FormHidden;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import lsfusion.http.provider.SessionInvalidatedException;
import net.customware.gwt.dispatch.server.ExecutionContext;

public class FormHiddenHandler extends FormActionHandler<FormHidden, VoidResult> {
    public FormHiddenHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(FormHidden action, ExecutionContext context) throws SessionInvalidatedException {
        removeFormSessionObject(action.formSessionID);
        return new VoidResult();
    }
}
