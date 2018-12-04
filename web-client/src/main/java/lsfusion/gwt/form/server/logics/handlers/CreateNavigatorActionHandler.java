package lsfusion.gwt.form.server.logics.handlers;

import lsfusion.gwt.form.server.logics.LogicsActionHandler;
import lsfusion.gwt.form.server.logics.spring.LogicsSessionObject;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.logics.CreateNavigator;
import lsfusion.gwt.form.shared.view.GNavigator;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.general.StringResult;

import java.io.IOException;

public class CreateNavigatorActionHandler extends LogicsActionHandler<CreateNavigator, StringResult> {
    public CreateNavigatorActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public StringResult executeEx(CreateNavigator action, ExecutionContext context) throws DispatchException, IOException {

        String logicsSID = action.logicsID;
        LogicsSessionObject logicsSessionObject = servlet.getLogicsSessionObject(logicsSID);
        GNavigator navigator = getLogicsProvider().createNavigator(logicsSID, logicsSessionObject, servlet.getNavigatorProvider());
        return new StringResult(navigator.sessionID);
    }
}
