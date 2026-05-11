package lsfusion.server.logics.action.flow;

import lsfusion.server.data.value.DataObject;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.navigator.controller.manager.NavigatorsManager;

/** Client-mode dispatch: NEWTHREAD inside a NEWEXECUTOR CLIENT conn scope. */
public class ClientFutureService extends ScheduledFutureService {
    private final DataObject clientConnection;
    private final NavigatorsManager navigatorsManager;

    public ClientFutureService(DataObject clientConnection, NavigatorsManager navigatorsManager, boolean awaited) {
        super(awaited);
        this.clientConnection = clientConnection;
        this.navigatorsManager = navigatorsManager;
    }

    public DataObject getClientConnection() {
        return clientConnection;
    }

    public NavigatorsManager getNavigatorsManager() {
        return navigatorsManager;
    }

    @Override
    public void shutdown() {
        // No server-side resources to release.
    }

    @Override
    public void interruptIfPossible(ExecutionContext<?> context) {
        // Notifications are dispatched through the navigator channel; nothing
        // server-side to interrupt.
    }
}
