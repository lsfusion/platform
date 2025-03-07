package lsfusion.server.logics.action.session.controller.remote;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.logics.form.interactive.controller.remote.serialization.ConnectionContext;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnectionContext;

import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

public class RemoteSessionContext extends RemoteConnectionContext {

    private final RemoteSession session;

    private final ConnectionContext remoteContext;

    @Override
    protected RemoteConnection getConnectionObject() {
        return session;
    }

    @Override
    public ConnectionContext getConnectionContext() {
        return remoteContext;
    }

    public RemoteSessionContext(RemoteSession session) {
        this.session = session;

        remoteContext = new ConnectionContext(true, false, false, false);
    }

    @Override
    public FocusListener getFocusListener() {
        return null;
    }

    @Override
    public CustomClassListener getClassListener() {
        return null;
    }
}
