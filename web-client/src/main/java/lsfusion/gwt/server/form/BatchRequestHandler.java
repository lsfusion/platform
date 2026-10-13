package lsfusion.gwt.server.form;

import lsfusion.gwt.client.controller.remote.action.form.FormRequestCountingAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.interop.form.remote.BatchRequest;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;
import java.util.Collections;

// the handler of a request a batch can carry: it makes the request as the form's remote interface takes it
// (getBatchRequest), and sends it alone as a batch of one - or ExecuteBatchHandler sends it with the rest of a batch
public abstract class BatchRequestHandler<A extends FormRequestCountingAction<ServerResponseResult>> extends FormServerResponseActionHandler<A> {

    protected BatchRequestHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    public abstract BatchRequest getBatchRequest(A action);

    @Override
    public ServerResponseResult executeEx(A action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.executeBatch(action.requestIndex, action.lastReceivedRequestIndex, Collections.singletonList(getBatchRequest(action))));
    }
}
