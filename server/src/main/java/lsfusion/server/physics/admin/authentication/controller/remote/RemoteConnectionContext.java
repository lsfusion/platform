package lsfusion.server.physics.admin.authentication.controller.remote;

import lsfusion.interop.connection.LocalePreferences;
import lsfusion.server.base.controller.context.AbstractContext;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.physics.admin.log.LogInfo;

import java.util.Locale;

public abstract class RemoteConnectionContext extends AbstractContext {
    
    protected abstract RemoteConnection getConnectionObject();

    @Override
    public LogInfo getLogInfo() {
        return getConnectionObject().getLogInfo();
    }

    @Override
    public Locale getLocale() {
        return getConnectionObject().getLocale();
    }

    @Override
    public LocalePreferences getLocalePreferences() {
        return getConnectionObject().getLocalePreferences();
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return getConnectionObject().logicsInstance;
    }

    @Override
    public Long getCurrentComputer() {
        return getConnectionObject().getCurrentComputer();
    }

    @Override
    public Long getCurrentConnection() {
        return getConnectionObject().getConnectionId();
    }

    @Override
    public Long getCurrentUser() {
        return getConnectionObject().getCurrentUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return getConnectionObject().userRole;
    }
}
