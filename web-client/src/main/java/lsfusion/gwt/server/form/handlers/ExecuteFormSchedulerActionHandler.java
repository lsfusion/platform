package lsfusion.gwt.server.form.handlers;

import lsfusion.gwt.client.controller.remote.action.form.ExecuteFormSchedulerAction;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.convert.GwtToClientConverter;
import lsfusion.gwt.server.form.FormServerResponseActionHandler;
import net.customware.gwt.dispatch.server.ExecutionContext;

import java.rmi.RemoteException;

public class ExecuteFormSchedulerActionHandler extends FormServerResponseActionHandler<ExecuteFormSchedulerAction> {

    private static GwtToClientConverter gwtConverter = GwtToClientConverter.getInstance();

    public ExecuteFormSchedulerActionHandler(MainDispatchServlet servlet) {
        super(servlet);
    }

    @Override
    public ServerResponseResult executeEx(final ExecuteFormSchedulerAction action, ExecutionContext context) throws RemoteException {
        return getServerResponseResult(action, remoteForm -> remoteForm.formSchedulerExecuted(action.requestIndex, action.lastReceivedRequestIndex, gwtConverter.convertOrCast(action.formScheduler)));
    }
}