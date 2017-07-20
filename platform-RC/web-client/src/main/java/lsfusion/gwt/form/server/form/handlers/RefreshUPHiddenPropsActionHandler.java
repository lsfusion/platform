package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.RefreshUPHiddenPropsAction;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class RefreshUPHiddenPropsActionHandler extends ServerResponseActionHandler<RefreshUPHiddenPropsAction> {

    public RefreshUPHiddenPropsActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(RefreshUPHiddenPropsAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.refreshUPHiddenProperties(action.requestIndex, -1, action.groupObjectSID, action.propSids));
    }
}
