package lsfusion.gwt.server.navigator.handlers;

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import lsfusion.base.ConcurrentIdentityWeakHashMap;
import lsfusion.base.ExceptionUtils;
import lsfusion.base.col.MapFact;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.actions.navigator.LogClientExceptionAction;
import lsfusion.gwt.shared.exceptions.NonFatalHandledException;
import lsfusion.gwt.shared.exceptions.RemoteInternalDispatchException;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.interop.exceptions.NonFatalRemoteClientException;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.navigator.RemoteNavigatorInterface;
import net.customware.gwt.dispatch.server.ExecutionContext;
import net.customware.gwt.dispatch.shared.DispatchException;

import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

import static lsfusion.gwt.server.GLoggers.invocationLogger;

public class LogClientExceptionActionHandler extends NavigatorActionHandler<LogClientExceptionAction, VoidResult> {
    public static final long COUNTER_CLEANER_PERIOD = 3 * 60 * 1000;
    
    private ConcurrentIdentityWeakHashMap<RemoteNavigatorInterface, Integer> exceptionCounter = MapFact.getGlobalConcurrentIdentityWeakHashMap();
    
    private StackTraceDeobfuscator deobfuscator;
    
    public LogClientExceptionActionHandler(MainDispatchServlet servlet) {
        super(servlet);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                exceptionCounter.clear();
            }
        }, COUNTER_CLEANER_PERIOD, COUNTER_CLEANER_PERIOD);
    }

    @Override
    public VoidResult executeEx(LogClientExceptionAction action, ExecutionContext context) throws RemoteException {
        RemoteNavigatorInterface navigator = getRemoteNavigator(action);

        Integer count = exceptionCounter.get(navigator);
        invocationLogger.info("Before logging exception, count : " + count + ", navigator " + navigator);
        
        if (count == null || count < 20) {
            Throwable throwable = action.throwable;
            
            if(!(throwable instanceof DispatchException)) // we don't need to deobfuscate server exception (it is a round trip exception, so in theory there is no client stack trace)
                getDeobfuscator().deobfuscateStackTrace(throwable, servlet.getRequest().getHeader(RpcRequestBuilder.STRONG_NAME_HEADER));

            throwable = fromWebServerToAppServer(throwable);

            try {
                navigator.logClientException(action.title, null, throwable);
            } finally {
                int newCount = count == null ? 1 : count + 1;
                exceptionCounter.put(navigator, newCount);
                invocationLogger.info("After logging exception, count : " + newCount + ", navigator " + navigator);
            }
        }
        
        return new VoidResult();
    }

    // result throwable class should exist on app-server
    public static Throwable fromWebServerToAppServer(Throwable throwable) {
        Throwable appThrowable;
        if (throwable instanceof RemoteInternalDispatchException)
            appThrowable = new RemoteInternalException(throwable.getMessage(), ((RemoteInternalDispatchException) throwable).lsfStack);
        else if(throwable instanceof NonFatalHandledException)
            appThrowable = new NonFatalRemoteClientException(throwable.getMessage(), ((NonFatalHandledException) throwable).count, ((NonFatalHandledException) throwable).reqId);
        else {
            assert throwable instanceof SerializableThrowable || throwable instanceof DispatchException;
            appThrowable = new Throwable(ExceptionUtils.copyMessage(throwable));
        }
        ExceptionUtils.copyStackTraces(throwable, appThrowable);
        return appThrowable;
    }

    private StackTraceDeobfuscator getDeobfuscator() {
        if (deobfuscator == null) {
            deobfuscator = StackTraceDeobfuscator.fromFileSystem(servlet.getServletContext().getRealPath("WEB-INF/deploy" + servlet.getRequestModuleBasePath() + "symbolMaps"));
        }
        return deobfuscator;
    }
}
