package lsfusion.server.form.navigator;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.remote.RemoteForm;

public class RemoteNavigatorContext extends AbstractContext {
    private final RemoteNavigator navigator;

    public RemoteNavigatorContext(RemoteNavigator navigator) {
        this.navigator = navigator;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return navigator.logicsInstance;
    }

    public String getLogMessage() {
        return navigator.getLogMessage();
    }

    @Override
    public LogInfo getLogInfo() {
        return navigator.getLogInfo();
    }

    public void delayUserInteraction(ClientAction action) {
        navigator.delayUserInteraction(action);
    }

    @Override
    public Object[] requestUserInteraction(final ClientAction... actions) {
        return navigator.requestUserInteraction(actions);
    }

    public SecurityPolicy getSecurityPolicy() {
        return navigator.securityPolicy;
    }

    public FocusListener getFocusListener() {
        return navigator;
    }

    public CustomClassListener getClassListener() {
        return navigator;
    }

    public PropertyObjectInterfaceInstance getComputer() {
        return navigator.getComputer();
    }

    public Integer getCurrentUser() {
        return (Integer) navigator.getUser().object;
    }

    public DataObject getConnection() {
        return navigator.getConnection();
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance) {
        try {
            return new RemoteForm(formInstance, navigator.getExportPort(), navigator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
