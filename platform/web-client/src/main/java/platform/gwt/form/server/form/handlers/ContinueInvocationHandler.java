package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.server.convert.GwtToClientConverter;
import platform.gwt.form.shared.actions.form.ContinueInvocation;
import platform.gwt.form.shared.actions.form.ServerResponseResult;

import java.io.IOException;

public class ContinueInvocationHandler extends ServerResponseActionHandler<ContinueInvocation> {
    private final GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ContinueInvocationHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(ContinueInvocation action, ExecutionContext context) throws DispatchException, IOException {
        FormSessionObject form = getFormSessionObject(action.formSessionID);

        Object actionResults[] = new Object[action.actionResults.length];
        for (int i = 0; i < actionResults.length; ++i) {
            actionResults[i] = gwtConverter.convertOrCast(action.actionResults[i]);
        }

        return getServerResponseResult(form, form.remoteForm.continueServerInvocation(actionResults));
    }
}
