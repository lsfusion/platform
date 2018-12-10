package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.shared.base.actions.VoidResult;
import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.form.actions.navigator.InterruptNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class InterruptNavigatorHandler extends NavigatorActionHandler<InterruptNavigator, VoidResult> {

    public InterruptNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(InterruptNavigator action, ExecutionContext context) throws DispatchException, IOException {
        getRemoteNavigator(action).interrupt(action.cancelable);
        return new VoidResult();
    }
}