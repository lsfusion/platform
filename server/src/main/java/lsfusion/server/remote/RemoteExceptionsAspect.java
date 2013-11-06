package lsfusion.server.remote;

import com.google.common.base.Throwables;
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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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

        OutputStream stackStream = new ByteArrayOutputStream();
        e.printStackTrace(new PrintStream(stackStream));
        String stackTrace = stackStream.toString();

        BusinessLogics BL = ThreadLocalContext.getBusinessLogics();
        try {
            BL.systemEventsLM.logException(BL, Throwables.getRootCause(e).getMessage(), e.getClass().getName(), stackTrace, null, null, false);
        } catch (SQLException sqle) {
            logger.error("Error when logging exception: ", sqle);
        }

        return new RemoteInternalException(0, e.getLocalizedMessage(), stackStream.toString());
    }
}
