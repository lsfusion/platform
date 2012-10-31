package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form2.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.actions.form.SetRegularFilter;

import java.io.IOException;

public class SetRegularFilterHandler extends ServerResponseActionHandler<SetRegularFilter> {
    public SetRegularFilterHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(SetRegularFilter action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.setRegularFilter(action.requestIndex, action.groupId, action.filterId));
    }
}
