package platform.gwt.main.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.main.server.RemoteServiceImpl;
import platform.gwt.main.shared.actions.form.ContinueInvocationAction;
import platform.gwt.main.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ContinueInvocationHandler extends ServerResponseActionHandler<ContinueInvocationAction> {
    public ContinueInvocationHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ContinueInvocationAction action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        Object actionResults[] = new Object[action.actionResults.length];
        for (int i = 0; i < actionResults.length; ++i) {
            actionResults[i] = action.actionResults[i].getValue();
        }

        return getServerResponseResult(form, form.remoteForm.continueServerInvocation(actionResults));
    }
}
