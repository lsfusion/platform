package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.shared.actions.NumberResult;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.GetInitialFilterProperty;
import platform.interop.form.RemoteDialogInterface;

import java.io.IOException;

public class GetInitialFilterPropertyHandler extends FormActionHandler<GetInitialFilterProperty, NumberResult> {
    public GetInitialFilterPropertyHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(GetInitialFilterProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        if (form.remoteForm instanceof RemoteDialogInterface) {
            return new NumberResult(((RemoteDialogInterface) form.remoteForm).getInitFilterPropertyDraw());
        }
        return null;
    }
}
