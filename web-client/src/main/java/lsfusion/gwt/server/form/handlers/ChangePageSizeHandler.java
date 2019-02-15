package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.shared.actions.form.ChangePageSize;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ChangePageSizeHandler extends FormServerResponseActionHandler<ChangePageSize> {
    public  ChangePageSizeHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ChangePageSize action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.changePageSize(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectID, action.pageSize));
    }
}
