package lsfusion.server.logics.form.interactive.controller.context;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.base.controller.remote.ui.RemoteUIContext;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.form.interactive.controller.remote.RemoteForm;
import lsfusion.server.logics.form.interactive.instance.FormInstance;
import lsfusion.server.logics.form.interactive.listener.CustomClassListener;
import lsfusion.server.logics.form.interactive.listener.FocusListener;
import lsfusion.server.logics.form.interactive.listener.RemoteFormListener;
import lsfusion.server.physics.admin.authentication.security.policy.SecurityPolicy;
import lsfusion.server.physics.admin.log.LogInfo;

import java.util.Locale;

public class RemoteFormContext<F extends FormInstance> extends RemoteUIContext {
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
        return form.form.getLogInfo();
    }

    public void aspectDelayUserInteraction(ClientAction action, String message) {
        form.delayUserInteraction(action, message);
    }

    @Override
    public Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        return form.requestUserInteraction(actions);
    }

    @Override
    protected int getExportPort() {
        return form.getExportPort();
    }

    @Override
    protected RemoteFormListener getFormListener() {
        return form.getRemoteFormListener();
    }

    @Override
    protected SecurityPolicy getSecurityPolicy() {
        return form.form.securityPolicy;
    }

    public FocusListener getFocusListener() {
        return form.form.getFocusListener();
    }

    public CustomClassListener getClassListener() {
        return form.form.getClassListener();
    }

    public Long getCurrentComputer() {
        return form.form.session.sql.contextProvider.getCurrentComputer();
    }

    public Long getCurrentConnection() {
        return form.form.session.sql.contextProvider.getCurrentConnection();
    }

    public Long getCurrentUser() {
        return form.form.session.sql.contextProvider.getCurrentUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return form.form.session.user.getCurrentUserRole();
    }

    @Override
    public Locale getLocale() {
        return form.form.getLocale();
    }
}
