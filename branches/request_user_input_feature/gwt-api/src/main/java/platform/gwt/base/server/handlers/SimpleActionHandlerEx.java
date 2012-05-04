package platform.gwt.base.server.handlers;

import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;
import platform.gwt.base.server.exceptions.IODispatchException;
import platform.gwt.base.server.exceptions.RemoteDispatchException;

import java.io.IOException;
import java.rmi.RemoteException;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result> extends SimpleActionHandler<A, R> {
    @Override
    public final R execute(A action, ExecutionContext context) throws DispatchException {
        try {
           return executeEx(action, context);
        } catch (RemoteException re) {
            throw new RemoteDispatchException("Удалённая ошибка при выполнении action: ", re);
        } catch (IOException ioe) {
            throw new IODispatchException("Ошибка ввода/вывода при выполнении action: ", ioe);
        }
    }

    public abstract R executeEx(A action, ExecutionContext context) throws DispatchException, IOException;
}
