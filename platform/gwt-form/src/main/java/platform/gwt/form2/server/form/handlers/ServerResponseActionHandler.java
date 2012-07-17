package platform.gwt.form2.server.form.handlers;

import net.customware.gwt.dispatch.shared.Action;
import platform.gwt.base.server.FormSessionObject;
import platform.gwt.form2.server.ClientToGwtConverter;
import platform.gwt.form2.server.RemoteServiceImpl;
import platform.gwt.form2.shared.actions.form.ServerResponseResult;
import platform.gwt.view2.actions.GAction;
import platform.interop.action.ClientAction;
import platform.interop.form.ServerResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class ServerResponseActionHandler<A extends Action<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientToGwtConverter clientConverter = ClientToGwtConverter.getInstance();

    protected ServerResponseActionHandler(RemoteServiceImpl servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) throws IOException {
        List<GAction> resultActions = new ArrayList<GAction>();

        for (ClientAction action : serverResponse.actions) {
            resultActions.add(clientConverter.convertAction(action, getSession(), form, servlet));
        }

        return new ServerResponseResult(resultActions.toArray(new GAction[resultActions.size()]), serverResponse.resumeInvocation);
    }
}
