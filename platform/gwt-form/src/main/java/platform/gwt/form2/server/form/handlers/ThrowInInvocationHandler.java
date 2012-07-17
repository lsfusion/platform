package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.form2.shared.actions.form.ThrowInInvocationAction;

import java.io.IOException;

public class ThrowInInvocationHandler extends ServerResponseActionHandler<ThrowInInvocationAction> {
    public ThrowInInvocationHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ThrowInInvocationAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);
        return getServerResponseResult(form, form.remoteForm.throwInServerInvocation(action.exception) );
    }
}
