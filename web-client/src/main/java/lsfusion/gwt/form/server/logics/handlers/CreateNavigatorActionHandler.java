package lsfusion.gwt.form.server.logics.handlers;

import lsfusion.gwt.form.server.logics.LogicsActionHandler;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.shared.actions.logics.CreateNavigator;
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
        servlet.createNavigator(action.tabSID);
        return new VoidResult();
    }
}
