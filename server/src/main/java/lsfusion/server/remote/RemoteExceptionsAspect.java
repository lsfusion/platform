package lsfusion.server.remote;

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
import java.sql.SQLException;

@Aspect
public class RemoteExceptionsAspect {
    private final static Logger logger = ServerLoggers.systemLogger;
    
    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around("execution(public * lsfusion.interop.RemoteLogicsInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.navigator.RemoteNavigatorInterface.*(..)) ||" +
            "execution(public * lsfusion.interop.form.RemoteFormInterface.*(..))")
    public Object executeRemoteMethod(ProceedingJoinPoint thisJoinPoint) throws Throwable {
        try {
            return thisJoinPoint.proceed();
        } catch (ThreadDeath td) {
            logger.error("Thread '" + Thread.currentThread() + "' was forcefully stopped.");
            throw new RemoteInternalException(1, "Thread was stopped");
        } catch (Throwable throwable) {
            if (!(throwable instanceof RemoteException) && !(throwable instanceof RemoteServerException)) {
                throw createInternalServerException(throwable);
            } else {
                throw throwable;
            }
        }
    }

    private static RemoteInternalException createInternalServerException(Throwable e) {
        logger.error("Internal server error: ", e);

        assert !(e.getCause() instanceof LoginException);

        BusinessLogics BL = ThreadLocalContext.getBusinessLogics();
        try {
            BL.systemEventsLM.logException(BL, e, null, null, false);
        } catch (Exception ex) {
            logger.error("Error when logging exception: ", ex);
        }

        return new RemoteInternalException(0, e.getLocalizedMessage());
    }
}
