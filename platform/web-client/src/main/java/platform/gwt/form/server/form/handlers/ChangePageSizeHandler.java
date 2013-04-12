package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormDispatchServlet;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.shared.actions.form.ChangePageSize;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangePageSizeHandler extends ServerResponseActionHandler<ChangePageSize> {
    public  ChangePageSizeHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangePageSize action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.changePageSize(action.requestIndex, action.groupObjectID, action.pageSize));
    }
}
