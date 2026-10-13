package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteBatch;
import lsfusion.gwt.client.controller.remote.action.form.FormRequestCountingAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.form.BatchRequestHandler;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import lsfusion.interop.form.remote.BatchRequest;
import net.customware.gwt.dispatch.server.ActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.InstanceActionHandlerRegistry;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

// several of the form's requests sent as one: each as its own handler makes it the remote interface's
// (BatchRequestHandler), all of them made by the form in their order, in one request
public class ExecuteBatchHandler extends FormServerResponseActionHandler<ExecuteBatch> {
    private final InstanceActionHandlerRegistry handlers; // where each request's own handler is

    public ExecuteBatchHandler(MainDispatchServlet servlet, InstanceActionHandlerRegistry handlers) {
        super(servlet);
        this.handlers = handlers;
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteBatch action, ExecutionContext context) throws RemoteException {
        List<BatchRequest> requests = new ArrayList<>();
        for (FormRequestCountingAction<?> request : action.requests)
            requests.add(getBatchRequest(request));
        return getServerResponseResult(action, remoteForm -> remoteForm.executeBatch(action.requestIndex, action.lastReceivedRequestIndex, requests));
    }

    // ... and one whose handler makes it no batch's (not a BatchRequestHandler) goes to the form all the same, to fail
    // the batch there (BatchRequest.Unsupported)
    @SuppressWarnings("unchecked")
    private <A extends FormRequestCountingAction<ServerResponseResult>> BatchRequest getBatchRequest(FormRequestCountingAction<?> request) {
        ActionHandler<?, ?> handler = handlers.findHandler(request);
        if (handler instanceof BatchRequestHandler) // a request of a kind a batch carries, answered by a ServerResponse
            return ((BatchRequestHandler<A>) handler).getBatchRequest((A) request);
        return new BatchRequest.Unsupported(request.getClass().getSimpleName());
    }
}
