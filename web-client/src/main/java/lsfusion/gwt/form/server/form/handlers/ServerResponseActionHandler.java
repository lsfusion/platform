package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.form.server.LSFusionDispatchServlet;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.server.convert.ClientActionToGwtConverter;
import lsfusion.gwt.form.shared.actions.form.ServerResponseResult;
import lsfusion.gwt.form.shared.view.actions.GAction;
import lsfusion.gwt.form.shared.view.actions.GThrowExceptionAction;
import lsfusion.interop.form.ServerResponse;
import net.customware.gwt.dispatch.shared.Action;

import java.io.IOException;

public abstract class ServerResponseActionHandler<A extends Action<ServerResponseResult>> extends FormActionHandler<A, ServerResponseResult> {
    private static ClientActionToGwtConverter clientActionConverter = ClientActionToGwtConverter.getInstance();

    protected ServerResponseActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);
    }

    protected ServerResponseResult getServerResponseResult(String tabSID, ServerResponse serverResponse) throws IOException {
        return getServerResponseResult(new FormSessionObject(null, null, tabSID), serverResponse);
    }

    protected ServerResponseResult getServerResponseResult(FormSessionObject form, ServerResponse serverResponse) throws IOException {
        GAction[] resultActions;
        if (serverResponse.actions == null) {
            resultActions = null;
        } else {
            resultActions = new GAction[serverResponse.actions.length];
            for (int i = 0; i < serverResponse.actions.length; i++) {
                try {
                    resultActions[i] = form != null
                              ? clientActionConverter.convertAction(serverResponse.actions[i], form, servlet)
                              : clientActionConverter.convertAction(serverResponse.actions[i], servlet);
                } catch (Exception e) {
                    resultActions[i] = new GThrowExceptionAction(new IllegalStateException("Can't convert server action: " + e.getMessage(), e));
                }
            }
        }

        return new ServerResponseResult(resultActions, serverResponse.resumeInvocation);
    }
}
