package lsfusion.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.GetRemoteActionMessage;

import java.io.IOException;

public class GetRemoteActionMessageHandler extends FormActionHandler<GetRemoteActionMessage, StringResult> {
    public GetRemoteActionMessageHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetRemoteActionMessage action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObjectOrNull(action.formSessionID);
        return new StringResult(form == null ? "" : form.remoteForm.getRemoteActionMessage());
    }
}
