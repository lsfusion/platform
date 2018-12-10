package lsfusion.gwt.server.form.form.handlers;

import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.form.provider.FormSessionObject;
import lsfusion.gwt.server.form.form.FormServerResponseActionHandler;
import lsfusion.gwt.shared.form.actions.form.RefreshUPHiddenPropsAction;
import lsfusion.gwt.shared.form.actions.form.ServerResponseResult;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class RefreshUPHiddenPropsActionHandler extends FormServerResponseActionHandler<RefreshUPHiddenPropsAction> {

    public RefreshUPHiddenPropsActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(RefreshUPHiddenPropsAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.refreshUPHiddenProperties(action.requestIndex, defaultLastReceivedRequestIndex, action.groupObjectSID, action.propSids));
    }
}
