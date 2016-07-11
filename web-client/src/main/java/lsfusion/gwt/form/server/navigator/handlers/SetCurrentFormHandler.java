package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.form.handlers.FormActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.SetCurrentForm;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class SetCurrentFormHandler extends FormActionHandler<SetCurrentForm, VoidResult> implements NavigatorActionHandler {
    public SetCurrentFormHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SetCurrentForm action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getNavigator().setCurrentForm(action.formID);
        return new VoidResult();
    }
}