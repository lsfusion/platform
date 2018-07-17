package lsfusion.gwt.form.server.form.handlers;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.ServerUtils;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.form.server.FormSessionObject;
import lsfusion.gwt.form.shared.actions.form.FormBoundAction;
import lsfusion.gwt.form.shared.actions.form.FormRequestIndexCountingAction;
import lsfusion.gwt.form.shared.actions.navigator.CleanAction;
import lsfusion.gwt.form.shared.actions.navigator.RegisterTabAction;
import lsfusion.interop.RemoteLogicsInterface;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.Result;

import static lsfusion.gwt.form.server.GLoggers.invocationLogger;

public abstract class LoggableActionHandler <A extends Action<R>, R extends Result, L extends RemoteLogicsInterface> extends SimpleActionHandlerEx<A, R, L> {
    
    public LoggableActionHandler(LogicsAwareDispatchServlet<L> servlet) {
        super(servlet);
    }
    
    @Override
    public void preExecute(A action) {
        invocationLogger.info("Executing action" + getActionDetails(action));    
    }
    
    @Override
    public void postExecute(A action) {
        invocationLogger.info("Executed action" + getActionDetails(action));
    }
    
    private String getActionDetails(A action) {
        String message = " by " + ServerUtils.getAuthorizedUserName() + ": ";
        if (this instanceof FormActionHandler && action instanceof FormBoundAction) {
            String formSessionID = ((FormBoundAction) action).formSessionID;
            FormSessionObject form = ((FormActionHandler) this).getFormSessionObjectOrNull(formSessionID);
            if (form != null) {
                message += form.clientForm.canonicalName + "(" + formSessionID + "):";
            }
        }
        message += action.getClass().getSimpleName();
        if (action instanceof FormRequestIndexCountingAction) {
            message += " : " + ((FormRequestIndexCountingAction) action).requestIndex;
        }
        if (action instanceof RegisterTabAction) {
            message += "TAB ID " + ((RegisterTabAction) action).tabSID + " IN " + servlet.getSessionInfo();
        }
        if (action instanceof CleanAction) {
            message += "TAB ID " + ((CleanAction) action).tabSID + " IN " + servlet.getSessionInfo();
        }
        return message;
    }
}
