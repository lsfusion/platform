package platform.gwt.form.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.form.server.FormSessionObject;
import platform.gwt.form.server.convert.ClientActionToGwtConverter;
import platform.gwt.form.server.RemoteServiceImpl;
import platform.gwt.form.shared.actions.form.ServerResponseResult;
import platform.gwt.form.shared.view.actions.GAction;
import platform.interop.action.ClientAction;
import platform.interop.form.ServerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ServerResponseActionHandler<A extends Action<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected ServerResponseActionHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) throws IOException {
        List<GAction> resultActions = new ArrayList<GAction>();

        for (ClientAction action : serverResponse.actions) {
            resultActions.add(clientActionConverter.convertAction(action, getSession(), form, servlet));
        }

        return new ServerResponseResult(resultActions.toArray(new GAction[resultActions.size()]), serverResponse.resumeInvocation, serverResponse.pendingRemoteChanges);
    }
}
