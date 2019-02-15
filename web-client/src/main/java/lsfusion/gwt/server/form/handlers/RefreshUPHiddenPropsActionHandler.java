package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.http.provider.form.FormSessionObject;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.actions.form.RefreshUPHiddenPropsAction;
import lsfusion.gwt.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class RefreshUPHiddenPropsActionHandler extends FormServerResponseActionHandler<RefreshUPHiddenPropsAction> {

    public RefreshUPHiddenPropsActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(RefreshUPHiddenPropsAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.refreshUPHiddenProperties(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectSID, action.propSids));
    }
}
