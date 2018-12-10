package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.server.form.FormActionHandler;
import lsfusion.gwt.form.shared.actions.form.Interrupt;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class InterruptHandler extends FormActionHandler<Interrupt, VoidResult> {

    public InterruptHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(Interrupt action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        if (form != null)
            form.remoteForm.interrupt(action.cancelable);
        return new VoidResult();
    }
}