package lsfusion.gwt.server.navigator.handlers;

import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.base.actions.VoidResult;
import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.navigator.SetCurrentForm;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class SetCurrentFormHandler extends NavigatorActionHandler<SetCurrentForm, VoidResult> {
    public SetCurrentFormHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(SetCurrentForm action, ExecutionContext context) throws DispatchException, IOException {
        getRemoteNavigator(action).setCurrentForm(action.formID);
        return new VoidResult();
    }
}