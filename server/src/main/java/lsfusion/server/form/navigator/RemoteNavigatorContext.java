package lsfusion.server.form.navigator;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.remote.RemoteConnection;
import lsfusion.server.remote.RemoteConnectionContext;
import lsfusion.server.remote.RemoteForm;

import java.util.Locale;

public class RemoteNavigatorContext extends RemoteConnectionContext {
    private final RemoteNavigator navigator;

    public RemoteNavigatorContext(RemoteNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    protected RemoteConnection getConnectionObject() {
        return navigator;
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        navigator.delayUserInteraction(action);
    }

    @Override
    protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        return navigator.requestUserInteraction(actions);
    }

    public FocusListener getFocusListener() {
        return navigator;
    }

    public CustomClassListener getClassListener() {
        return navigator;
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        try {
            return new RemoteForm(formInstance, navigator.getExportPort(), navigator, stack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
