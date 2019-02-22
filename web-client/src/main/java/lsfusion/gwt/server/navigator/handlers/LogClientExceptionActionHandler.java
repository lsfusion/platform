package lsfusion.gwt.server.navigator.handlers;

import com.google.gwt.core.server.StackTraceDeobfuscator;
import com.google.gwt.core.shared.SerializableThrowable;
import com.google.gwt.user.client.rpc.RpcRequestBuilder;
import lsfusion.base.ConcurrentIdentityWeakHashMap;
import lsfusion.base.col.MapFact;
import lsfusion.gwt.server.MainDispatchServlet;
import lsfusion.gwt.server.navigator.NavigatorActionHandler;
import lsfusion.gwt.shared.actions.navigator.LogClientExceptionAction;
import lsfusion.gwt.shared.exceptions.RemoteInternalDispatchException;
import lsfusion.gwt.shared.result.VoidResult;
import lsfusion.interop.exceptions.NonFatalHandledRemoteException;
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
            Throwable throwable;
            Throwable actionThrowable = action.throwable;
            if(actionThrowable instanceof SerializableThrowable) {
                throwable = new Throwable(actionThrowable.getMessage());

                if (action.nonFatal) { // created from NonFatalHandledException (was wrapped because there could be problems with causes)
                    RemoteException remoteException = new RemoteException(actionThrowable.toString());
                    remoteException.setStackTrace(new StackTraceElement[]{});
                    throwable = new NonFatalHandledRemoteException(remoteException, action.reqId);
                    ((NonFatalHandledRemoteException) throwable).count = action.count;
                }

                throwable.setStackTrace(actionThrowable.getStackTrace());
                getDeobfuscator().deobfuscateStackTrace(throwable, servlet.getRequest().getHeader(RpcRequestBuilder.STRONG_NAME_HEADER));
            } else {
                assert !action.nonFatal;
                assert actionThrowable instanceof DispatchException;
                if(actionThrowable instanceof RemoteInternalDispatchException) {
                    throwable = new RemoteInternalException(actionThrowable, ((RemoteInternalDispatchException) actionThrowable).javaStack, ((RemoteInternalDispatchException) actionThrowable).lsfStack);
                } else {
                    throwable = new Throwable(actionThrowable);
                    throwable.setStackTrace(actionThrowable.getStackTrace());
                    // we don't need to obfuscate since it is a round trip exception
                }
            }

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
    
    private StackTraceDeobfuscator getDeobfuscator() {
        if (deobfuscator == null) {
            deobfuscator = StackTraceDeobfuscator.fromFileSystem(servlet.getServletContext().getRealPath("WEB-INF/deploy" + servlet.getRequestModuleBasePath() + "symbolMaps"));
        }
        return deobfuscator;
    }
}
