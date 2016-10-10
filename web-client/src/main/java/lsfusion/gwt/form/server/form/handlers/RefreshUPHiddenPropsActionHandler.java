package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.RefreshUPHiddenPropsAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class RefreshUPHiddenPropsActionHandler extends FormActionHandler<RefreshUPHiddenPropsAction, VoidResult> {

    public RefreshUPHiddenPropsActionHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(RefreshUPHiddenPropsAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        form.remoteForm.refreshUPHiddenProperties(action.requestIndex, -1, action.propSids);
        return new VoidResult();
    }
}
