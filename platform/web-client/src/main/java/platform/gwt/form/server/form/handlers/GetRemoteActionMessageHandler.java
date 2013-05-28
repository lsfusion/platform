package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.GetRemoteActionMessage;

import java.io.IOException;

public class GetRemoteActionMessageHandler extends FormActionHandler<GetRemoteActionMessage, StringResult> {
    public GetRemoteActionMessageHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(GetRemoteActionMessage action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return new StringResult(form.remoteForm.getRemoteActionMessage());
    }
}
