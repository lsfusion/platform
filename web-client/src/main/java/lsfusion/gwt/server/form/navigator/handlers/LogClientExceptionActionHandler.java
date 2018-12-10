package lsfusion.gwt.server.form.navigator.handlers;

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import lsfusion.base.ConcurrentIdentityWeakHashMap;
import lsfusion.base.col.MapFact;
import lsfusion.gwt.server.form.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.base.actions.VoidResult;
import lsfusion.gwt.server.form.spring.LSFusionDispatchServlet;
import lsfusion.gwt.shared.form.actions.navigator.LogClientExceptionAction;
import lsfusion.interop.exceptions.NonFatalHandledRemoteException;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import static lsfusion.gwt.server.form.GLoggers.invocationLogger;

public class LogClientExceptionActionHandler extends NavigatorActionHandler<LogClientExceptionAction, VoidResult> {
    public static final long COUNTER_CLEANER_PERIOD = 3 * 60 * 1000;
    
    private ConcurrentIdentityWeakHashMap<RemoteNavigatorInterface, Integer> exceptionCounter = MapFact.getGlobalConcurrentIdentityWeakHashMap();
    
    private GStackTraceDeobfuscator deobfuscator = new GStackTraceDeobfuscator();
    
    public LogClientExceptionActionHandler(LSFusionDispatchServlet servlet) {
        super(servlet);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                exceptionCounter.clear();
            }
        }, COUNTER_CLEANER_PERIOD, COUNTER_CLEANER_PERIOD);
    }

    @Override
    public VoidResult executeEx(LogClientExceptionAction action, ExecutionContext context) throws DispatchException, IOException {
        RemoteNavigatorInterface navigator = getRemoteNavigator(action);

        // чтобы не засорять Журнал ошибок, ограничиваем количество отчётов об ошибках от одного пользователя.
        Integer count = exceptionCounter.get(navigator);
        invocationLogger.info("Before logging exception, count : " + count + ", navigator " + navigator);
        
        if (count == null || count < 20) {
            Throwable throwable = new Throwable(action.throwable.toString());

            if (action.nonFatal) {
                RemoteException remoteException = new RemoteException(action.throwable.toString());
                remoteException.setStackTrace(new StackTraceElement[]{});
                throwable = new NonFatalHandledRemoteException(remoteException, action.reqId);
                ((NonFatalHandledRemoteException) throwable).count = action.count;
            }
            
            throwable.setStackTrace(action.throwable.getStackTrace());
            
            deobfuscator.deobfuscateStackTrace(throwable, servlet.getRequest().getHeader(RpcRequestBuilder.STRONG_NAME_HEADER));
            navigator.logClientException(action.title, null, throwable);

            int newCount = count == null ? 1 : count + 1;
            exceptionCounter.put(navigator, newCount);

            invocationLogger.info("After logging exception, count : " + newCount + ", navigator " + navigator);
        }
        
        return new VoidResult();
    }
    
    class GStackTraceDeobfuscator extends StackTraceDeobfuscator {
        @Override
        protected InputStream openInputStream(String fileName) throws IOException {
            return new FileInputStream(new File(servlet.getServletContext().getRealPath("WEB-INF/deploy/form/symbolMaps"), fileName));
        }
    }
}
