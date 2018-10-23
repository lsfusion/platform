package lsfusion.server.remote;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;

import java.util.Locale;

public class RemoteFormContext<F extends FormInstance> extends AbstractContext {
    private final RemoteForm<F> form;

    public RemoteFormContext(RemoteForm<F> form) {
        this.form = form;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return form.form.logicsInstance;
    }

    public FormInstance getFormInstance() {
        return form.form;
    }

    @Override
    public LogInfo getLogInfo() {
        return form.getLogInfo();
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        form.delayUserInteraction(action);
    }

    @Override
    protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        return form.requestUserInteraction(actions);
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        try {
            return new RemoteForm<>(formInstance, form.getExportPort(), form.getRemoteFormListener(), stack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public SecurityPolicy getSecurityPolicy() {
        return form.form.securityPolicy;
    }

    public FocusListener getFocusListener() {
        return form.form.getFocusListener();
    }

    public CustomClassListener getClassListener() {
        return form.form.getClassListener();
    }

    public PropertyObjectInterfaceInstance getComputer(ExecutionStack stack) {
        return form.form.instanceFactory.computer;
    }

    public Long getCurrentUser() {
        return form.getCurrentUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return form.getCurrentUserRole();
    }

    public DataObject getConnection() {
        return form.form.instanceFactory.connection;
    }

    @Override
    public Locale getLocale() {
        return form.form.getLocale();
    }
}
