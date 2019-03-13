package lsfusion.server.remote;

import lsfusion.server.base.context.AbstractContext;
import lsfusion.server.logics.navigator.LogInfo;
import lsfusion.server.logics.LogicsInstance;

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
    public LogicsInstance getLogicsInstance() {
        return getConnectionObject().logicsInstance;
    }

    @Override
    public Long getCurrentComputer() {
        return getConnectionObject().getCurrentComputer();
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
