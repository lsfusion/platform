package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.base.shared.actions.BooleanResult;
import lsfusion.gwt.form.shared.actions.navigator.ForbidDuplicateFormsAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ForbidDuplicateFormsHandler extends SimpleActionHandlerEx<ForbidDuplicateFormsAction, BooleanResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    public ForbidDuplicateFormsHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public BooleanResult executeEx(ForbidDuplicateFormsAction action, ExecutionContext context) throws DispatchException, IOException {
        return new BooleanResult(servlet.getNavigator().isForbidDuplicateForms());
    }
}