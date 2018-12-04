package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.gwt.base.shared.actions.BooleanResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.form.shared.actions.navigator.ForbidDuplicateFormsAction;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class ForbidDuplicateFormsHandler extends NavigatorActionHandler<ForbidDuplicateFormsAction, BooleanResult> {
    public ForbidDuplicateFormsHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public BooleanResult executeEx(ForbidDuplicateFormsAction action, ExecutionContext context) throws DispatchException, IOException {
        return new BooleanResult(servlet.getNavigator().isForbidDuplicateForms());
    }
}