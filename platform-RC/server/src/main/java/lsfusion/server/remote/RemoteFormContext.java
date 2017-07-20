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
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;

import java.util.Locale;

public class RemoteFormContext<T extends BusinessLogics<T>, F extends FormInstance<T>> extends AbstractContext {
    private final RemoteForm<T, F> form;

    public RemoteFormContext(RemoteForm<T, F> form) {
        this.form = form;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return form.form.logicsInstance;
    }

    public FormInstance getFormInstance() {
        return form.form;
    }

    public String getLogMessage() {
        return form.getLogMessage();
    }

    @Override
    public LogInfo getLogInfo() {
        return form.getLogInfo();
    }

    public void delayUserInteraction(ClientAction action) {
        form.delayUserInteraction(action);
    }

    public Object[] requestUserInteraction(ClientAction... actions) {
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

    public Integer getCurrentUser() {
        return form.getCurrentUser();
    }

    public DataObject getConnection() {
        return form.form.instanceFactory.connection;
    }

    @Override
    public Locale getLocale() {
        return form.form.getLocale();
    }
}
