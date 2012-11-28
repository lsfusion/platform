package platform.gwt.paas.server.handlers;

import com.gwtplatform.dispatch.server.ExecutionContext;
import com.gwtplatform.dispatch.server.actionhandler.ActionHandler;
import com.gwtplatform.dispatch.shared.Action;
import com.gwtplatform.dispatch.shared.ActionException;
import com.gwtplatform.dispatch.shared.Result;
import org.springframework.security.core.Authentication;
import platform.base.ReflectionUtils;
import platform.gwt.base.server.ServerUtils;
import platform.gwt.paas.server.exceptions.IOActionException;
import platform.gwt.paas.server.exceptions.RemoteActionException;
import platform.gwt.paas.shared.exceptions.MessageException;
import platform.interop.exceptions.RemoteMessageException;

import java.io.IOException;
import java.rmi.RemoteException;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result> implements ActionHandler<A, R> {
    private final Class<A> actionType;

    public SimpleActionHandlerEx() {
        actionType = ReflectionUtils.getFirstTypeParameterOfSuperclass(getClass());
    }

    public SimpleActionHandlerEx(Class<A> actionType) {
        this.actionType = actionType;
    }

    public Class<A> getActionType() {
        return actionType;
    }

    @Override
    public final R execute(A action, ExecutionContext context) throws ActionException {
        try {
            return executeEx(action, context);
        } catch (RemoteMessageException rme) {
            throw new MessageException(rme.getMessage());
        } catch (RemoteException re) {
            throw new RemoteActionException("Удалённая ошибка при выполнении action: ", re);
        } catch (IOException ioe) {
            throw new IOActionException("Ошибка ввода/вывода при выполнении action: ", ioe);
        }
    }

    public abstract R executeEx(A action, ExecutionContext context) throws ActionException, IOException;

    @Override
    public void undo(A action, R result, ExecutionContext context) throws ActionException {
    }

    protected Authentication getAuthentication() {
        return ServerUtils.getAuthentication();
    }
}
