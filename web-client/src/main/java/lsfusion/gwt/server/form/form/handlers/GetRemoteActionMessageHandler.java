package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.GetRemoteActionMessage;

import java.io.IOException;

public class GetRemoteActionMessageHandler extends FormActionHandler<GetRemoteActionMessage, StringResult> {
    public GetRemoteActionMessageHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    protected String getActionDetails(GetRemoteActionMessage action) {
        return null; // too many logs
    }

    @Override
    public StringResult executeEx(GetRemoteActionMessage action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new StringResult(form == null ? "" : form.remoteForm.getRemoteActionMessage());
    }
}
