package lsfusion.server.base.remote;

import lsfusion.base.ExceptionUtils;
import lsfusion.interop.exception.RemoteInternalException;
import lsfusion.interop.exception.RemoteMessageException;
import lsfusion.interop.exception.RemoteServerException;
import lsfusion.server.physics.admin.logging.ServerLoggers;
import lsfusion.server.base.thread.ThreadLocalContext;
import lsfusion.server.base.remote.context.ContextAwarePendingRemoteObject;
import lsfusion.server.base.remote.context.RemoteContextAspect;
import lsfusion.server.base.stack.ThrowableWithStack;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.rmi.RemoteException;

@Aspect
public class RemoteExceptionsAspect {
    private final static Logger logger = ServerLoggers.systemLogger;
    
    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around(RemoteContextAspect.allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        try {
            return thisJoinPoint.proceed();
        } catch (Throwable throwable) {
            boolean suppressLog = throwable instanceof RemoteInternalException; // "nested remote call" so we don't need to log it twice
            if(throwable instanceof ThreadDeath || throwable instanceof InterruptedException) {
                logger.error("Thread '" + Thread.currentThread() + "' was forcefully stopped.");
                suppressLog = true; // we don't need that situation, because if client ran some really long action and exited, all his threads will be stopped eventually, and then we'll get a lot of that exceptions
            }

            throwable = fromAppServerToWebServerAndDesktopClient(throwable);

            if(!suppressLog)
                logException(throwable, (ContextAwarePendingRemoteObject) target);
            
            throw throwable;
        }
    }

    public void logException(Throwable throwable, ContextAwarePendingRemoteObject target) {
        if(throwable instanceof RemoteInternalException) {                                
            try {
                target.logServerException((RemoteInternalException) throwable);
            } catch (Throwable t) {
                logger.error("Error when logging exception: ", t);
            }
        }
    }

    // result throwable class should exist both on web-server and on desktop-client
    private static Throwable fromAppServerToWebServerAndDesktopClient(Throwable e) {
        // this classes exist both on web-server and desktop-client and unlikely to have causes
        if(e instanceof RemoteException || e instanceof RemoteServerException)
            return e;

        ThrowableWithStack throwableWithStack = new ThrowableWithStack(e);

        Throwable throwable = throwableWithStack.getThrowable();
        if(throwableWithStack.isNoStackRequired())
            return new RemoteMessageException(throwable.getMessage());

        RemoteInternalException result = new RemoteInternalException(ThreadLocalContext.localize("{exceptions.internal.server.error}")
                                    + ": " + ExceptionUtils.copyMessage(throwable), throwableWithStack.getLsfStack());
        ExceptionUtils.copyStackTraces(throwable, result);
        return result;
    }
}
