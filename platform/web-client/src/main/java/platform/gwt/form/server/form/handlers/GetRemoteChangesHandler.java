package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.GetRemoteChanges;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class GetRemoteChangesHandler extends ServerResponseActionHandler<GetRemoteChanges> {
    public GetRemoteChangesHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(GetRemoteChanges action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.getRemoteChanges(action.requestIndex));
    }
}
