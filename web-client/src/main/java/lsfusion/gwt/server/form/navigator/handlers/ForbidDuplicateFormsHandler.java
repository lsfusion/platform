package lsfusion.gwt.server.form.navigator.handlers;

import lsfusion.gwt.shared.base.actions.BooleanResult;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.form.actions.navigator.ForbidDuplicateFormsAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ForbidDuplicateFormsHandler extends NavigatorActionHandler<ForbidDuplicateFormsAction, BooleanResult> {
    public ForbidDuplicateFormsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BooleanResult executeEx(ForbidDuplicateFormsAction action, ExecutionContext context) throws DispatchException, IOException {
        return new BooleanResult(getRemoteNavigator(action).isForbidDuplicateForms());
    }
}