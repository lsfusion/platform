package lsfusion.gwt.base.server.dispatch;

import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.exceptions.IODispatchException;
import lsfusion.gwt.base.server.exceptions.RemoteDispatchException;
import lsfusion.gwt.base.server.exceptions.RemoteFormDispatchException;
import lsfusion.gwt.base.server.exceptions.RemoteNavigatorDispatchException;
import lsfusion.gwt.base.shared.MessageException;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.exceptions.RemoteMessageException;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.server.SimpleActionHandler;
import net.customware.gwt.dispatch.shared.Action;
import net.customware.gwt.dispatch.shared.DispatchException;
import net.customware.gwt.dispatch.shared.Result;

import java.io.IOException;
import java.rmi.NoSuchObjectException;

public abstract class SimpleActionHandlerEx<A extends Action<R>, R extends Result, L extends RemoteLogicsInterface> extends SimpleActionHandler<A, R> {
    protected final LogicsAwareDispatchServlet<L> servlet;

    public SimpleActionHandlerEx(LogicsAwareDispatchServlet<L> servlet) {
        this.servlet = servlet;
    }

    @Override
    public final R execute(A action, ExecutionContext context) throws DispatchException {
        try {
           return executeEx(action, context);
        } catch (RemoteMessageException rme) {
            throw new MessageException(rme.getMessage());
        } catch (NoSuchObjectException re) {
            if (this instanceof BaseFormBoundActionHandler && !(this instanceof NavigatorActionHandler)) {
                throw new RemoteFormDispatchException("Удалённая ошибка при выполнении action: ", re);
            } else if (this instanceof NavigatorActionHandler) {
                throw new RemoteNavigatorDispatchException("Удалённая ошибка при выполнении action: ", re);
            } else {
                throw new RemoteDispatchException("Удалённая ошибка при выполнении action: ", re);
            }
        } catch (IOException ioe) {
            throw new IODispatchException("Ошибка ввода/вывода при выполнении action: ", ioe);
        }
    }

    public abstract R executeEx(A action, ExecutionContext context) throws DispatchException, IOException;
}
