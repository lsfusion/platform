package lsfusion.server.remote;

import lsfusion.server.stack.ThrowableWithStack;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.exceptions.RemoteServerException;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;

import java.rmi.RemoteException;

@Aspect
public class RemoteExceptionsAspect {
    private final static Logger logger = ServerLoggers.systemLogger;
    
    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around(RemoteContextAspect.allRemoteCalls)
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint, Object target) throws Throwable {
        try {
            return thisJoinPoint.proceed();
        } catch (ThreadDeath | InterruptedException td) {
            logger.error("Thread '" + Thread.currentThread() + "' was forcefully stopped.");
            throw new RemoteInternalException("Thread was stopped", td, false, null);
        } catch (Throwable throwable) {
            if (!(throwable instanceof RemoteException) && !(throwable instanceof RemoteServerException)) {
                throw createInternalServerException(throwable, (ContextAwarePendingRemoteObject)target);
            } else {
                throw throwable;
            }
        }
    }

    private static RemoteInternalException createInternalServerException(Throwable e, ContextAwarePendingRemoteObject target) {
        ThrowableWithStack throwableWithStack = new ThrowableWithStack(e);
        throwableWithStack.log("Internal server error", logger);

        RemoteInternalException internalException = new RemoteInternalException(ThreadLocalContext.localize("{exceptions.internal.server.error}"), throwableWithStack.getThrowable(), throwableWithStack.isNoStackRequired(), throwableWithStack.getLsfStack());
        try {
            target.logServerException(internalException);
        } catch (Exception ex) {
            logger.error("Error when logging exception: ", ex);
        }
        return internalException;
    }
}
