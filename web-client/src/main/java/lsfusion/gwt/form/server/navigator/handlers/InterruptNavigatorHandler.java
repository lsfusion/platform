package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.InterruptNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class InterruptNavigatorHandler extends NavigatorActionHandler<InterruptNavigator, VoidResult> {

    public InterruptNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(InterruptNavigator action, ExecutionContext context) throws DispatchException, IOException {
        servlet.getNavigator().interrupt(action.cancelable);
        return new VoidResult();
    }
}