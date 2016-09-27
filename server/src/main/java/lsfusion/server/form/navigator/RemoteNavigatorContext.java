package lsfusion.server.form.navigator;

import lsfusion.interop.LocalePreferences;
import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.context.AbstractContext;
import lsfusion.server.context.ExecutionStack;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.PropertyObjectInterfaceInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.remote.RemoteForm;

import java.util.Locale;

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

    public PropertyObjectInterfaceInstance getComputer(ExecutionStack stack) {
        return navigator.getComputer();
    }

    public Integer getCurrentUser() {
        return (Integer) navigator.getUser().object;
    }

    public DataObject getConnection() {
        return navigator.getConnection();
    }

    @Override
    public Locale getLocale() {
        LocalePreferences pref = navigator.getLocalLocalePreferences();
        if (pref != null && pref.useClientLocale && pref.language != null) {
            return new Locale(pref.language, pref.country == null ? "" : pref.country);
        } 
        return Locale.getDefault();
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
