package platform.gwt.form.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.RemoteFormServiceImpl;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

public class GetFormHandler extends FormActionHandler<GetForm> {
    public GetFormHandler(RemoteFormServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public GetFormResult executeEx(GetForm action, ExecutionContext context) throws DispatchException, IOException {
        String sid = action.sid != null && !action.sid.isEmpty()
                     ? action.sid
                     : "connectionsForm";

        RemoteFormInterface remoteForm = servlet.getNavigator().createForm(sid, false, true);
        return createResult(remoteForm);
    }
}
