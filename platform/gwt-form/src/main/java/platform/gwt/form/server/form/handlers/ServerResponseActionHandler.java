package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.server.convert.ClientActionToGwtConverter;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.actions.GAction;
import platform.interop.form.ServerResponse;

import java.io.IOException;

public abstract class ServerResponseActionHandler<A extends Action<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected ServerResponseActionHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) throws IOException {
        GAction[] resultActions;
        if (serverResponse.actions == null) {
            resultActions = null;
        } else {
            resultActions = new GAction[serverResponse.actions.length];
            for (int i = 0; i < serverResponse.actions.length; i++) {
                resultActions[i] = clientActionConverter.convertAction(serverResponse.actions[i], getSession(), form, servlet);
            }
        }

        return new ServerResponseResult(resultActions, serverResponse.resumeInvocation, serverResponse.pendingRemoteChanges);
    }
}
