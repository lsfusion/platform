package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ChangePageSize;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangePageSizeHandler extends FormServerResponseActionHandler<ChangePageSize> {
    public  ChangePageSizeHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangePageSize action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.changePageSize(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID, action.pageSize));
    }
}
