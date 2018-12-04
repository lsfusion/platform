package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.SetCurrentForm;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class SetCurrentFormHandler extends NavigatorActionHandler<SetCurrentForm, VoidResult> {
    public SetCurrentFormHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SetCurrentForm action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getNavigator().setCurrentForm(action.formID);
        return new VoidResult();
    }
}