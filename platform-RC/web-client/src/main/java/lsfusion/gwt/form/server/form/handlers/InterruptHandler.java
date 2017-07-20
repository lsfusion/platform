package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.FormDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.Interrupt;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class InterruptHandler extends FormActionHandler<Interrupt, VoidResult> {

    public InterruptHandler(FormDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(Interrupt action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObjectOrNull(action.formSessionID);
        if (form != null)
            form.remoteForm.interrupt(action.cancelable);
        return new VoidResult();
    }
}