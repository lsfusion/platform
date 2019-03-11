package lsfusion.server.remote;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.LogicsInstance;

import java.util.Locale;

import static lsfusion.server.ServerLoggers.systemLogger;

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
    protected void aspectDelayUserInteraction(ClientAction action, String message) {
        if(message != null)
            systemLogger.info("Server message: " + message);
        else
            throw new UnsupportedOperationException("delayUserInteraction is not supported in session context, action : " + action.getClass());
    }

    @Override
    protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
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
