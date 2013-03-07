package platform.server.remote;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import platform.interop.exceptions.LoginException;
import platform.interop.exceptions.RemoteInternalException;
import platform.interop.exceptions.RemoteServerException;
import platform.server.context.ThreadLocalContext;
import platform.server.logics.BusinessLogics;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.sql.SQLException;

@Aspect
public class RemoteExceptionsAspect {
    private final static Logger logger = Logger.getLogger(RemoteExceptionsAspect.class);
    
    // аспектами ловим все RuntimeException которые доходят до внешней границы сервера и оборачиваем их
    @Around("execution(public * platform.interop.RemoteLogicsInterface.*(..)) ||" +
            "execution(public * platform.interop.navigator.RemoteNavigatorInterface.*(..)) ||" +
            "execution(public * platform.interop.form.RemoteDialogInterface.*(..)) ||" +
            "execution(public * platform.interop.form.RemoteFormInterface.*(..))")
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
