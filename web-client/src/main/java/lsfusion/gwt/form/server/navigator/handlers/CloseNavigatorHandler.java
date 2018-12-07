package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.navigator.CloseNavigator;
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
