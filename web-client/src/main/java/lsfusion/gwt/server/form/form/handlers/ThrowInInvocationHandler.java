package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.spring.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import lsfusion.gwt.form.server.form.spring.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.actions.form.ThrowInInvocation;

import java.io.IOException;

public class ThrowInInvocationHandler extends FormServerResponseActionHandler<ThrowInInvocation> {
    public ThrowInInvocationHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInInvocation action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.throwInServerInvocation(-1, defaultLastReceivedRequestIndex, -1, action.throwable) );
    }
}
