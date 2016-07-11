package lsfusion.gwt.form.server.navigator.handlers;

import lsfusion.base.ConcurrentIdentityWeakHashMap;
import lsfusion.base.col.MapFact;
import lsfusion.gwt.base.server.LogicsAwareDispatchServlet;
import lsfusion.gwt.base.server.dispatch.NavigatorActionHandler;
import lsfusion.gwt.base.server.dispatch.SimpleActionHandlerEx;
import lsfusion.gwt.base.shared.actions.VoidResult;
import lsfusion.gwt.form.shared.actions.navigator.LogClientExceptionAction;
import lsfusion.interop.RemoteLogicsInterface;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.IOException;

public class LogClientExceptionActionHandler extends SimpleActionHandlerEx<LogClientExceptionAction, VoidResult, RemoteLogicsInterface> implements NavigatorActionHandler {
    private ConcurrentIdentityWeakHashMap<RemoteNavigatorInterface, Integer> exceptionCounter = MapFact.getGlobalConcurrentIdentityWeakHashMap();
    
    public LogClientExceptionActionHandler(LogicsAwareDispatchServlet<RemoteLogicsInterface> servlet) {
        super(servlet);
    }

    @Override
    public VoidResult executeEx(LogClientExceptionAction action, ExecutionContext context) throws DispatchException, IOException {
        RemoteNavigatorInterface navigator = servlet.getNavigator();

        // чтобы не засорять Журнал ошибок, ограничиваем количество отчётов об ошибках от одного пользователя.
        Integer count = exceptionCounter.get(navigator);
        if (count == null || count < 20) {
            navigator.logClientException(action.title, null, new Throwable(action.throwable));
            exceptionCounter.put(navigator, count == null ? 1 : count + 1);
        }
        
        return new VoidResult();
    }
}
