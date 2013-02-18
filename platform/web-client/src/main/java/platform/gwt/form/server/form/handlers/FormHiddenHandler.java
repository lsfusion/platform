package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.shared.actions.VoidResult;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.FormHidden;

import java.io.IOException;

public class FormHiddenHandler extends FormActionHandler<FormHidden, VoidResult> {
    public FormHiddenHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(FormHidden action, ExecutionContext context) throws DispatchException, IOException {
        getFormSessionManager().removeFormSessionObject(action.formSessionID);
        return new VoidResult();
    }
}
