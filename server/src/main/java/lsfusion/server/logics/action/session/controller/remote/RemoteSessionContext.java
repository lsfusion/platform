package lsfusion.server.logics.action.session.controller.remote;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnection;
import lsfusion.server.physics.admin.authentication.controller.remote.RemoteConnectionContext;

import static lsfusion.server.physics.admin.log.ServerLoggers.systemLogger;

public class RemoteSessionContext extends RemoteConnectionContext {

    private final RemoteSession session;

    @Override
    protected RemoteConnection getConnectionObject() {
        return session;
    }

    public RemoteSessionContext(RemoteSession session) {
        this.session = session;
    }

    @Override
    public void aspectDelayUserInteraction(ClientAction action, String message) {
        if(message != null)
            systemLogger.info("Server message: " + message);
        else
            throw new UnsupportedOperationException("delayUserInteraction is not supported in session context, action : " + action.getClass());
    }

    @Override
    public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            String message = messages[i];
            if (message == null)
                throw new UnsupportedOperationException("requestUserInteraction is not supported in session context, action : " + actions[i].getClass());
        }
        return new Object[actions.length];
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
