package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.shared.actions.NumberResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.GetInitialFilterProperty;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class GetInitialFilterPropertyHandler extends FormActionHandler<GetInitialFilterProperty, NumberResult> {
    public GetInitialFilterPropertyHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public NumberResult executeEx(GetInitialFilterProperty action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return form.remoteForm == null ? null : new NumberResult(form.remoteForm.getInitFilterPropertyDraw());
    }
}
