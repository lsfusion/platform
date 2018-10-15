package lsfusion.server.remote;

import lsfusion.server.stack.ExecutionStackAspect;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import lsfusion.interop.exceptions.LoginException;
import lsfusion.interop.exceptions.RemoteInternalException;
import lsfusion.interop.exceptions.RemoteServerException;
import lsfusion.server.ServerLoggers;
import lsfusion.server.context.ThreadLocalContext;
import lsfusion.server.logics.BusinessLogics;

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
            throw new RemoteInternalException("Thread was stopped", null);
        } catch (Throwable throwable) {
            if (!(throwable instanceof RemoteException) && !(throwable instanceof RemoteServerException)) {
                throw createInternalServerException(throwable, (ContextAwarePendingRemoteObject)target);
            } else {
                throw throwable;
            }
        }
    }

    private static RemoteInternalException createInternalServerException(Throwable e, ContextAwarePendingRemoteObject target) {
        logger.error("Internal server error: ", e);

        String lsfExceptionStack = ExecutionStackAspect.getExceptionStackString();
        try {
            target.logServerException(e, lsfExceptionStack);
        } catch (Exception ex) {
            logger.error("Error when logging exception: ", ex);
        }

        return new RemoteInternalException(e.getLocalizedMessage(), lsfExceptionStack);
    }
}
