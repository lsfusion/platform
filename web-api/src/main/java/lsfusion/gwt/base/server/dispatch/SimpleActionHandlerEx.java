package lsfusion.gwt.base.server.dispatch;

import lsfusion.base.ExceptionUtils;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.exceptions.IODispatchException;
import lsfusion.gwt.base.server.exceptions.RemoteRetryException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.RemoteMessageException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.rmi.RemoteException;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result, L extends RemoteLogicsInterface> extends SimpleActionHandler<A, R> {
    protected final LogicsAwareDispatchServlet<L> servlet;

    public SimpleActionHandlerEx(LogicsAwareDispatchServlet<L> servlet) {
        this.servlet = servlet;
    }

    @Override
    public final R execute(A action, ExecutionContext context) throws DispatchException {
        try {
            preExecute(action);
            R result = executeEx(action, context);
            postExecute(action);
            return result;
        } catch (RemoteMessageException rme) {
            throw new MessageException(rme.getMessage());
        } catch (IOException ioe) {
            if (ioe instanceof RemoteException && !(ioe.getCause() instanceof ClassNotFoundException)) { // пробуем послать повторный запрос
                throw new RemoteRetryException(ioe.getMessage(), (RemoteException) ioe, ExceptionUtils.getFatalRemoteExceptionCount(ioe));
            }
            throw new IODispatchException("Ошибка ввода/вывода при выполнении action: ", ioe);
        }
    }

    public void preExecute(A action) {}

    public void postExecute(A action) {}

    public abstract R executeEx(A action, ExecutionContext context) throws DispatchException, IOException;
}
