package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.server.LSFusionDispatchServlet;
import lsfusion.gwt.server.form.FormActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.gwt.shared.actions.form.FormHidden;

import java.io.IOException;

public class FormHiddenHandler extends FormActionHandler<FormHidden, VoidResult> {
    public FormHiddenHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(FormHidden action, ExecutionContext context) throws DispatchException, IOException {
        removeFormSessionObject(action.formSessionID);
        return new VoidResult();
    }
}
