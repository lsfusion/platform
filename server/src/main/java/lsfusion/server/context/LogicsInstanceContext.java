package lsfusion.server.context;

import lsfusion.interop.action.ClientAction;
import lsfusion.server.auth.SecurityPolicy;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.form.instance.listener.CustomClassListener;
import lsfusion.server.form.instance.listener.FocusListener;
import lsfusion.server.form.navigator.LogInfo;
import lsfusion.server.logics.LogicsInstance;
import lsfusion.server.logics.SecurityManager;
import lsfusion.server.remote.RemoteForm;
import org.apache.log4j.Logger;

import java.util.*;

import static lsfusion.server.ServerLoggers.systemLogger;

public class LogicsInstanceContext extends AbstractContext {
    private static final Logger logger = Logger.getLogger(LogicsInstanceContext.class);

    private final LogicsInstance logicsInstance;

    public LogicsInstanceContext(LogicsInstance logicsInstance) {
        this.logicsInstance = logicsInstance;
    }

    @Override
    public LogicsInstance getLogicsInstance() {
        return logicsInstance;
    }

    public SecurityPolicy getSecurityPolicy() {
        return SecurityManager.serverSecurityPolicy;
    }

    public FocusListener getFocusListener() {
        return null;
    }

    public CustomClassListener getClassListener() {
        return null;
    }

    public Long getCurrentComputer() {
        return logicsInstance.getDbManager().getServerComputer();
    }

    public Long getCurrentUser() {
        return logicsInstance.getDbManager().getSystemUser();
    }

    @Override
    public Long getCurrentUserRole() {
        return null;
    }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public RemoteForm createRemoteForm(FormInstance formInstance, ExecutionStack stack) {
        throw new UnsupportedOperationException("createRemoteForm is not supported in server context");
    }

    @Override
    public LogInfo getLogInfo() {
        return LogInfo.system;
    }

    @Override
    public void aspectDelayUserInteraction(ClientAction action, String message) {
        if(message != null)
            systemLogger.info("Server message: " + message);
        else
            throw new UnsupportedOperationException("delayUserInteraction is not supported in server context, action : " + action.getClass());
    }

    @Override
    protected Object[] aspectRequestUserInteraction(ClientAction[] actions, String[] messages) {
        for (int i = 0; i < messages.length; i++) {
            String message = messages[i];
            if (message == null)
                throw new UnsupportedOperationException("requestUserInteraction is not supported in server context, action : " + actions[i].getClass());
        }
        return new Object[actions.length];
    }

    @Override
    public boolean canBeProcessed() {
        return true;
    }
}
