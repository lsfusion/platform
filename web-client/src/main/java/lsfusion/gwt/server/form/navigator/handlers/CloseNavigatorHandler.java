package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.base.actions.VoidResult;
import lsfusion.gwt.server.form.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.navigator.CloseNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class  CloseNavigatorHandler extends NavigatorActionHandler<CloseNavigator, VoidResult> {
    public CloseNavigatorHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(CloseNavigator action, ExecutionContext context) throws DispatchException, IOException {
        removeLogicsAndNavigatorSessionObject(action.sessionID);
        return new VoidResult();
    }
}
